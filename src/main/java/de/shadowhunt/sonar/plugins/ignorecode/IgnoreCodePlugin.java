package de.shadowhunt.sonar.plugins.ignorecode;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.SonarPlugin;

import de.shadowhunt.sonar.plugins.ignorecode.measures.IgnoreMeasureFilter;
import de.shadowhunt.sonar.plugins.ignorecode.rules.IgnoreViolationsFilter;

public class IgnoreCodePlugin extends SonarPlugin {

	@Override
	@SuppressWarnings("deprecation")
	public List<Class<?>> getExtensions() {
		return Arrays.asList(IgnoreViolationsFilter.class, IgnoreMeasureFilter.class);
	}
}
