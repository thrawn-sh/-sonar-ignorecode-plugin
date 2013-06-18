package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;

import de.shadowhunt.sonar.plugins.ignorecode.model.LinePattern;
import de.shadowhunt.sonar.plugins.ignorecode.util.LinePatternDecoder;

/**
 * Generated code must not fulfill all requirements as coverage, complexity, ...
 * Therefore the we go through all resources and remove/update all measures
 */
abstract class AbstractDecorator implements Decorator {

	protected final Map<String, Set<Integer>> ignores = new HashMap<String, Set<Integer>>();

	protected final void loadIgnores() {
		final String fileLocation = getConfigurationLocation();
		if (StringUtils.isBlank(fileLocation)) {
			return;
		}

		final Collection<LinePattern> patterns = LinePatternDecoder.loadLinePatterns(fileLocation);
		for (final LinePattern pattern : patterns) {
			ignores.put(pattern.getResource(), pattern.getLines());
		}
	}

	@CheckForNull
	protected abstract String getConfigurationLocation();

	@Override
	public boolean shouldExecuteOnProject(final Project project) {
		return Java.INSTANCE.equals(project.getLanguage()) && StringUtils.isNotBlank(getConfigurationLocation());
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}
}
