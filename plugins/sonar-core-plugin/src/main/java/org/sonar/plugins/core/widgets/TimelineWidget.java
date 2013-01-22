/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.widgets;

import org.sonar.api.web.WidgetCategory;
import org.sonar.api.web.WidgetProperties;
import org.sonar.api.web.WidgetProperty;
import org.sonar.api.web.WidgetPropertyType;

@WidgetCategory("History")
@WidgetProperties({
  @WidgetProperty(key = "chartTitle", type = WidgetPropertyType.STRING, description = "chartTitle description"),
  @WidgetProperty(key = "metric1", type = WidgetPropertyType.METRIC, defaultValue = "ncloc", options = {WidgetConstants.FILTER_OUT_NEW_METRICS}, description = "metric1 description"),
  @WidgetProperty(key = "metric2", type = WidgetPropertyType.METRIC, options = {WidgetConstants.FILTER_OUT_NEW_METRICS}, description = "metric2 description"),
  @WidgetProperty(key = "metric3", type = WidgetPropertyType.METRIC, options = {WidgetConstants.FILTER_OUT_NEW_METRICS}, description = "metric3 description"),
  @WidgetProperty(key = "hideEvents", type = WidgetPropertyType.BOOLEAN, description = "hideEvents description"),
  @WidgetProperty(key = "hideEvents2", type = WidgetPropertyType.BOOLEAN, description = "hideEvents description", defaultValue = "true"),
  @WidgetProperty(key = "chartHeight", type = WidgetPropertyType.INTEGER, defaultValue = "80", description = "chartHeight description"),

  @WidgetProperty(key = "text", type = WidgetPropertyType.TEXT, description = "text description"),
  @WidgetProperty(key = "password", type = WidgetPropertyType.PASSWORD, description = "password description", defaultValue = "123"),
  @WidgetProperty(key = "password2", type = WidgetPropertyType.PASSWORD, description = "password2 description"),
  @WidgetProperty(key = "float", type = WidgetPropertyType.FLOAT, description = "float description"),
  @WidgetProperty(key = "regex", type = WidgetPropertyType.REGULAR_EXPRESSION, description = "regex description"),
  @WidgetProperty(key = "filter", type = WidgetPropertyType.FILTER, description = "filter description"),
  @WidgetProperty(
    key = "list",
    defaultValue = "2",
    type = WidgetPropertyType.SINGLE_SELECT_LIST,
    options = {"1", "2", "3"},
    description = "list description"),
    @WidgetProperty(
        key = "list2",
        type = WidgetPropertyType.SINGLE_SELECT_LIST,
        options = {"1", "2", "3"},
        description = "list2 description")
})
public class TimelineWidget extends CoreWidget {
  public TimelineWidget() {
    super("timeline", "Timeline", "/org/sonar/plugins/core/widgets/timeline.html.erb");
  }
}
