package caravan.debug;

import caravan.CaravanApplication;
import caravan.components.CameraFocusC;
import caravan.components.Components;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.components.RenderC;
import caravan.services.EntitySpawnService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.darkyen.retinazer.Component;
import com.darkyen.retinazer.Engine;
import com.darkyen.retinazer.EntitySetView;
import com.darkyen.retinazer.Mapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Function;

/**
 * A window widget for entity inspection and real-time value editing.
 */
public final class EntityEditorWindow extends Window {

	private static final String LOG = "EntityEditorWindow";

	private final Tree<Node, Object> entityTree;

	private Engine engine;
	private EntitySetView playerEntities;
	private Mapper<PlayerC> playerMapper;

	public void setWorld(Engine engine) {
		this.engine = engine;
		this.playerMapper = engine.getMapper(PlayerC.class);
		this.playerEntities = engine.getEntities(Components.DOMAIN.familyWith(PlayerC.class, PositionC.class));
	}

	private int getPlayerEntity() {
		final IntArray playerEntities = this.playerEntities.getIndices();
		for (int i = 0; i < playerEntities.size; i++) {
			final int entity = playerEntities.items[i];
			final PlayerC playerC = playerMapper.get(entity);
			if (playerC.selected) {
				return entity;
			}
		}
		return -1;
	}

	private int selectedEntity = -1;

	public EntityEditorWindow(Skin skin) {
		super("Entity Editor", skin);

		final Table root = new Table(skin);
		entityTree = new Tree<>(skin);
		final ScrollPane entityTreeScroll = new ScrollPane(entityTree);

		add(root).expand().fill();

		button(root, "Self", () -> {
			selectedEntity = getPlayerEntity();
			refreshEntityView();
		});
		final Vector2 nearestVector1 = new Vector2();
		final Vector2 nearestVector2 = new Vector2();
		button(root, "Nearest", () -> {
			final Mapper<PositionC> positionMapper = engine.getMapper(PositionC.class);
			final int tracked = getPlayerEntity();

			if (tracked == -1) {
				selectedEntity = -1;
				refreshEntityView();
				return;
			}
			final PositionC positionC = positionMapper.getOrNull(tracked);
			if (positionC == null) return;
			final Vector2 trackedPosition = nearestVector1.set(positionC.x, positionC.y);

			int nearest = -1;
			float nearestDist2 = Float.MAX_VALUE;

			final IntArray entitiesArray = engine.getEntities().getIndices();
			final int[] entities = entitiesArray.items;
			final int entitiesSize = entitiesArray.size;
			for (int i = 0; i < entitiesSize; i++) {
				final int entity = entities[i];

				if (entity == tracked) continue;

				final PositionC position = positionMapper.getOrNull(entity);
				if (position == null) continue;

				final float entityDst2 = nearestVector2.set(position.x, position.y).dst2(trackedPosition);
				if (entityDst2 < nearestDist2) {
					nearest = entity;
					nearestDist2 = entityDst2;
				}
			}

			selectedEntity = nearest;
			refreshEntityView();
		});
		button(root, "Drop Beacon", () -> {
			final Mapper<PositionC> positionMapper = engine.getMapper(PositionC.class);
			final int tracked = getPlayerEntity();

			if (tracked == -1) {
				selectedEntity = -1;
				refreshEntityView();
				return;
			}
			final PositionC positionC = positionMapper.getOrNull(tracked);
			if (positionC == null) return;
			final Vector2 trackedPosition = nearestVector1.set(positionC.x, positionC.y);

			final Engine e = engine;
			final int entity = e.createEntity();
			e.getMapper(PositionC.class).create(entity).set(trackedPosition.x, trackedPosition.y);
			e.getMapper(RenderC.class).create(entity).set(EntitySpawnService.Flowers[0], 1f, 1f);
			e.getMapper(CameraFocusC.class).create(entity).set(1f);
		});
		button(root, "Remove", () -> {
			if (selectedEntity != -1) {
				engine.destroyEntity(selectedEntity);
				selectedEntity = -1;
				refreshEntityView();
			}
		}).row();

		root.add(entityTreeScroll).colspan(4).expand().fill();

		setResizable(true);
		pack();
	}

	private void refreshEntityView() {
		entityTree.clearChildren();
		final Array<Object> onStack = new Array<>();
		final int selectedEntity = this.selectedEntity;
		if (selectedEntity != -1) {
			final Skin skin = CaravanApplication.uiSkin();
			final Node rootNode = new Node(new Label(Integer.toString(selectedEntity), skin));
			final Mapper<?>[] mappers = engine.getMappers();
			for (Mapper<? extends Component> mapper : mappers) {
				final Component component = mapper.getOrNull(selectedEntity);

				if (component == null) continue;
				final Node componentNode = new Node(new Label(component.getClass().getSimpleName()+" - "+System.identityHashCode(component), skin));

				try {
					for (Field field : component.getClass().getFields()) {
						if (Modifier.isStatic(field.getModifiers())) continue;
                        try {
                            onStack.add(component);
                            componentNode.add(createFieldNode(onStack, field));
                        } catch (Exception ex) {
                            Gdx.app.error("EntityEditorWindow", "Failed to create node view for field " + field.getName() + " of component " + componentNode, ex);
                        } finally {
                            onStack.clear();
                        }
					}
				} catch (Exception ex) {
					Gdx.app.error("EntityEditorWindow", "Failed to create node view for component " + componentNode, ex);
				}

				rootNode.add(componentNode);
			}
			rootNode.setExpanded(true);
			entityTree.add(rootNode);
		}
		if (getPrefWidth() > getWidth()) {
			setWidth(getPrefWidth());
		}
		if (getPrefHeight() > getHeight()) {
			setHeight(getPrefHeight());
		}
	}

	private Node createFieldNode(Array<Object> onStack, Field field) throws IllegalAccessException {
		final Object on = onStack.peek();
		final Skin skin = CaravanApplication.uiSkin();
		final Class<?> fieldType = field.getType();
		final String fieldName = field.getName();

		if (fieldType == boolean.class || fieldType == Boolean.class) {
			final CheckBox cb = new CheckBox(fieldName, skin);
			cb.setChecked(field.getBoolean(on));
			cb.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					try {
						field.setBoolean(on, cb.isChecked());
					} catch (Exception ex) {
						Gdx.app.error(LOG, "Failed to set boolean", ex);
					}
				}
			});
			return new Node(cb);
		} else if (fieldType == byte.class || fieldType == Byte.class) {
			return this.createPrimitiveEditNode(fieldName, field, on, (i) -> "0x" + Integer.toHexString(0xFF & i), s -> {
				try {
					if (s.startsWith("0x")) {
						if (s.length() == 2) return (byte) 0;
						return (byte) Integer.parseInt(s.substring(2), 16);
					} else {
						if (s.isEmpty()) return null;
						return (byte) Integer.parseInt(s);
					}
				} catch (NumberFormatException e) {
					return null;
				}
			});
		} else if (fieldType == char.class || fieldType == Character.class) {
			return this.createPrimitiveEditNode(fieldName, field, on, (i) -> "0x" + Integer.toHexString(i), s -> {
				try {
					if (s.startsWith("0x")) {
						return (char) Short.parseShort(s.substring(2), 16);
					} else if (s.startsWith("'")) {
						return s.charAt(1);
					} else {
						return (char) Short.parseShort(s);
					}
				} catch (IndexOutOfBoundsException | NumberFormatException e) {
					return null;
				}
			});
		} else if (fieldType == short.class || fieldType == Short.class) {
			return this.createPrimitiveEditNode(fieldName, field, on, Object::toString, s -> {
				try {
					return Short.parseShort(s);
				} catch (NumberFormatException e) {
					return null;
				}
			});
		} else if (fieldType == int.class || fieldType == Integer.class) {
			return this.createPrimitiveEditNode(fieldName, field, on, Object::toString, s -> {
				try {
					return Integer.parseInt(s);
				} catch (NumberFormatException e) {
					return null;
				}
			});
		} else if (fieldType == long.class || fieldType == Long.class) {
			return this.createPrimitiveEditNode(fieldName, field, on, Object::toString, s -> {
				try {
					return Long.parseLong(s);
				} catch (NumberFormatException e) {
					return null;
				}
			});
		} else if (fieldType == float.class || fieldType == Float.class) {
			return this.createPrimitiveEditNode(fieldName, field, on, Object::toString, s -> {
				try {
					return Float.parseFloat(s);
				} catch (NumberFormatException e) {
					return null;
				}
			});
		} else if (fieldType == double.class || fieldType == Double.class) {
			return this.createPrimitiveEditNode(fieldName, field, on, Object::toString, s -> {
				try {
					return Double.parseDouble(s);
				} catch (NumberFormatException e) {
					return null;
				}
			});
		} else if (fieldType == String.class) {
			return this.createPrimitiveEditNode(fieldName, field, on, Object::toString, s -> s);
		} else {
			//Compound type
			final Object compoundFieldValue = field.get(on);
			if (onStack.contains(compoundFieldValue, true)) {
				return new Node(new Label(fieldName + " (loop)", skin));
			} else {
				final Node compoundNode = new Node(new Label(fieldName, skin));
				for (Field compoundField : fieldType.getFields()) {
					onStack.add(compoundFieldValue);
					compoundNode.add(createFieldNode(onStack, compoundField));
					onStack.pop();
				}
				return compoundNode;
			}
		}
	}

	private <T> String convertToString(Object v, Function<T, String> toString) {
		try {
			//noinspection unchecked
			return toString.apply((T) v);
		} catch (Exception ex) {
			return "<" + v + ">";
		}
	}

	private <T> Node createPrimitiveEditNode(String title, Field field, Object on, Function<T, String> toString, Function<String, T> fromString) throws IllegalAccessException {
		final TextField tf = new TextField(convertToString(field.get(on), toString), CaravanApplication.uiSkin());
		tf.setTextFieldListener((textField, c) -> {
			if (c == '\n' || c == '\r' || c == '\t') {
				textField.getStage().setKeyboardFocus(null);

				final T converted = fromString.apply(tf.getText());
				if (converted == null) {
					try {
						tf.setText(convertToString(field.get(on), toString));
					} catch (IllegalAccessException e) {
						Gdx.app.error(LOG, "Failed to reset value of \"" + title + "\"");
					}
				} else {
					try {
						field.set(on, converted);
					} catch (IllegalAccessException e) {
						Gdx.app.error(LOG, "Failed to set value of \"" + title + "\" to " + converted);
					}
				}
			}
		});
		return wrapInLabelledNode(tf, title);
	}

	private Node wrapInLabelledNode(Actor wrapped, String title) {
		final Skin skin = CaravanApplication.uiSkin();
		final Table table = new Table(skin);
		table.add(title).center().fillX().expandX().row();
		table.add(wrapped).fillX().expandX().row();
		return new Node(table);
	}

	private Cell<TextButton> button(Table table, String label, Runnable onClick) {
		final TextButton button = new TextButton(label, getSkin());
		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				onClick.run();
			}
		});
		return table.add(button);
	}

	private static final class Node extends Tree.Node<Node, Object, Actor> {
		public Node(Actor actor) {
			super(actor);
		}
	}
}
