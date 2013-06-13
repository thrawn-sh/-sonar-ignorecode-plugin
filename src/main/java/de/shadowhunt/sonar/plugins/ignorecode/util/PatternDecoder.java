package de.shadowhunt.sonar.plugins.ignorecode.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.SonarException;

import de.shadowhunt.sonar.plugins.ignorecode.model.IgnorePattern;

public final class PatternDecoder {

	private static final String CONFIG_FILE = "sonar.switchoffviolations.configFile";

	private static final String LINE_RANGE_REGEXP = "\\[((\\d+|\\d+-\\d+),?)*\\]";

	static List<IgnorePattern> decodeFile(final File file) {
		try {
			final List<IgnorePattern> patterns = new ArrayList<IgnorePattern>();
			for (final String line : Files.readAllLines(file.toPath(), Charset.defaultCharset())) {
				final IgnorePattern pattern = decodeLine(line);
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
	static IgnorePattern decodeLine(final String line) {
		if (isBlankOrComment(line)) {
			return null;
		}

		final String[] fields = StringUtils.split(line, ';');
		if (fields.length != 3) {
			throw new SonarException("Invalid format. The following line does not define 3 fields separated by ';': "
					+ line);
		}

		final String resourcePattern = StringUtils.trim(fields[0]);
		if (!isResourcePattern(resourcePattern)) {
			throw new SonarException("Invalid format. The first field does not define a resource pattern: " + line);
		}

		final String rulePattern = StringUtils.trim(fields[1]);
		if (!isRulePattern(rulePattern)) {
			throw new SonarException("Invalid format. The second field does not define a rule pattern: " + line);
		}

		final String lines = fields[2];
		if (!isLinesRange(lines)) {
			throw new SonarException("Invalid format. The third field does not define a range of lines: " + line);
		}

		return IgnorePattern.createIgnorePattern(resourcePattern, rulePattern, lines, line);
	}

	static boolean isBlankOrComment(final String line) {
		return StringUtils.isBlank(line) || (line.charAt(0) == '#');
	}

	static boolean isLinesRange(final String field) {
		return StringUtils.equals(field, "*") || Pattern.matches(LINE_RANGE_REGEXP, field);
	}

	static boolean isResourcePattern(final String field) {
		return StringUtils.isNotBlank(field);
	}

	static boolean isRulePattern(final String field) {
		return StringUtils.isNotBlank(field);
	}

	public static List<IgnorePattern> loadIgnorePatterns(final Configuration configuration) {
		final String fileLocation = configuration.getString(CONFIG_FILE);
		if (StringUtils.isNotBlank(fileLocation)) {
			final File file = locateFile(fileLocation);
			return PatternDecoder.decodeFile(file);
		}
		return Collections.emptyList();
	}

	static File locateFile(final String location) {
		final File file = new File(location);
		if (!file.exists() || !file.isFile()) {
			throw new SonarException("File not found. Please check the parameter " + CONFIG_FILE + ": " + location);
		}

		return file;
	}

	private PatternDecoder() {
		// hide constructor
	}
}
