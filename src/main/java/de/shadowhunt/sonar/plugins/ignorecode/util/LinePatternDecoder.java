package de.shadowhunt.sonar.plugins.ignorecode.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.SonarException;

import de.shadowhunt.sonar.plugins.ignorecode.model.LinePattern;

public class LinePatternDecoder {

	private static final String LINE_RANGE_REGEXP = "\\[((\\d+|\\d+-\\d+),?)*\\]";

	static List<LinePattern> decodeFile(final File file) {
		try {
			final List<LinePattern> patterns = new ArrayList<LinePattern>();
			for (final String line : Files.readAllLines(file.toPath(), Charset.defaultCharset())) {
				final LinePattern pattern = decodeLine(line);
				if (pattern != null) {
					patterns.add(pattern);
				}
			}
			return patterns;
		} catch (final IOException ioe) {
			throw new SonarException("Fail to load file: " + file.getAbsolutePath(), ioe);
		}
	}

	@CheckForNull
	static LinePattern decodeLine(final String line) {
		if (isBlankOrComment(line)) {
			return null;
		}

		final String[] fields = StringUtils.split(line, ';');
		if (fields.length != 2) {
			throw new SonarException("Invalid format. The following line does not define 2 fields separated by ';': "
					+ line);
		}

		final String resource = fields[0];
		if (!StringUtils.isNotBlank(resource)) {
			throw new SonarException("Invalid format. The first field does not define a resource pattern: " + line);
		}

		final String lines = fields[1];
		if (!isLinesRange(lines)) {
			throw new SonarException("Invalid format. The second field does not define a range of lines: " + line);
		}

		return LinePattern.createLinePattern(resource, lines);
	}

	static boolean isBlankOrComment(final String line) {
		return StringUtils.isBlank(line) || (line.charAt(0) == '#');
	}

	static boolean isLinesRange(final String field) {
		return (field != null) && Pattern.matches(LINE_RANGE_REGEXP, field);
	}

	public static Collection<LinePattern> loadLinePatterns(final String location) {
		if (StringUtils.isNotBlank(location)) {
			final File file = locateFile(location);
			final List<LinePattern> patterns = decodeFile(file);
			return LinePattern.merge(patterns);
		}
		return Collections.emptyList();
	}

	static File locateFile(final String location) {
		final File file = new File(location);
		if (!file.exists() || !file.isFile()) {
			throw new SonarException("File not found: " + location);
		}

		return file;
	}

	private LinePatternDecoder() {
		// hide constructor
	}
}
