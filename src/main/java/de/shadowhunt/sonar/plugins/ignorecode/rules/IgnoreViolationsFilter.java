package de.shadowhunt.sonar.plugins.ignorecode.rules;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.rules.ViolationFilter;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.WildcardPattern;

import de.shadowhunt.sonar.plugins.ignorecode.model.ViolationPattern;

public class IgnoreViolationsFilter implements ViolationFilter {

	public static final String CONFIG_FILE = "sonar.switchoffviolations.configFile"; // FIXME change to: ignoreviolations

	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreViolationsFilter.class);

	static final List<ViolationPattern> loadPatterns(final Configuration configuration) {
		if (configuration == null) {
			return Collections.emptyList();
		}

		final String fileLocation = configuration.getString(CONFIG_FILE);
		if (StringUtils.isBlank(fileLocation)) {
			LOGGER.info("no ignore file configured for property: " + CONFIG_FILE);
			return Collections.emptyList();
		}

		final File ignoreFile = new File(fileLocation);
		if (!ignoreFile.isFile()) {
			LOGGER.error("could not find ignore file: " + ignoreFile);
			return Collections.emptyList();
		}

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(ignoreFile);
			return ViolationPattern.parse(fis);
		} catch (final Exception e) {
			throw new SonarException("could not load ignores for file: " + ignoreFile, e);
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}

	static boolean match(final Violation violation, final ViolationPattern pattern) {
		final boolean isMatchingResource = matchResource(violation.getResource(), pattern.getResourcePattern());
		if (!isMatchingResource) {
			return false;
		}

		final boolean isMatchingRule = matchRule(violation.getRule(), pattern.getRulePattern());
		if (!isMatchingRule) {
			return false;
		}

		final Set<Integer> lines = pattern.getLines();
		if (lines.isEmpty()) {
			return true; // empty is any line
		}
		return lines.contains(violation.getLineId());
	}

	static boolean matchResource(final Resource<?> resource, final String pattern) {
		return WildcardPattern.create(pattern).match(resource.getKey());
	}

	static boolean matchRule(final Rule rule, final String pattern) {
		return WildcardPattern.create(pattern).match(rule.getRepositoryKey() + ":" + rule.getKey());
	}

	private final List<ViolationPattern> patterns;

	public IgnoreViolationsFilter(final Configuration configuration) {
		patterns = loadPatterns(configuration);
	}

	@Override
	public boolean isIgnored(final Violation violation) {
		final boolean debugEnabled = LOGGER.isDebugEnabled();
		for (final ViolationPattern pattern : patterns) {
			if (match(violation, pattern)) {
				if (debugEnabled) {
					LOGGER.debug("Violation " + violation + " switched off by " + pattern);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}
}
