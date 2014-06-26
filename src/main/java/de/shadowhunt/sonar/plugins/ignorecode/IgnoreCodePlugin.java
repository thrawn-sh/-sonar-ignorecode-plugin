/**
 * This file is part of Sonar Ignore Code Plugin.
 *
 * Sonar Ignore Code Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sonar Ignore Code Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sonar Ignore Code Plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.shadowhunt.sonar.plugins.ignorecode;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.SonarPlugin;

import de.shadowhunt.sonar.plugins.ignorecode.batch.IgnoreCoverageDecorator;
import de.shadowhunt.sonar.plugins.ignorecode.rules.IgnoreViolationsFilter;

/**
 * Register all {@code Extension}s
 */
public class IgnoreCodePlugin extends SonarPlugin {

	@Override
	@SuppressWarnings({ "deprecation", "rawtypes" })
	public List getExtensions() {
		return Arrays.asList(IgnoreCoverageDecorator.class, IgnoreViolationsFilter.class);
	}
}
