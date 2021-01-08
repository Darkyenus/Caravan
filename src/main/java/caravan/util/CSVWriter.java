package caravan.util;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

public final class CSVWriter implements Closeable {

	private final Writer out;
	private final String separator;

	private int firstRowItems = -1;
	private int rowItems = 0;
	private int row = 1;

	public CSVWriter(Writer out, String separator, boolean forWindowsOS) throws IOException {
		this.out = out;
		this.separator = separator;
		if (forWindowsOS) {
			// Byte Order Mark
			// UTF8 encoded as: 0xEF, 0xBB, 0xBF
			out.write(0xFEFF);
		}
	}

	public CSVWriter(Writer out) throws IOException {
		this(out, ",", false);
	}

	public void item(@Nullable String value) throws IOException {
		if (rowItems > 0) {
			out.write(separator);
		}
		rowItems++;

		if (value == null) {
			return;
		}

		if (value.indexOf('"') != -1 || value.indexOf('\n') != -1 || value.indexOf('\r') != -1 || value.contains(separator)) {
			out.write((int) '"');
			for (int i = 0; i < value.length(); i++) {
				final char c = value.charAt(i);
				out.append(c);
				if (c == '"') {
					out.append('"');
				}
			}
			out.write('"');
		} else {
			out.write(value);
		}
	}

	public void row() throws IOException {
		if (firstRowItems == -1) {
			firstRowItems = rowItems;
		} else if (firstRowItems != rowItems) {
			System.err.println("Row "+row+" has "+rowItems+" items, while the header specifies "+firstRowItems+" items");
		}
		rowItems = 0;
		row++;

		out.write("\r\n");
	}

	@Override
	public void close() throws IOException {
		out.close();
	}
}
