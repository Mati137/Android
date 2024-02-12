/*
 * Copyright (c) 2024 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.networkprotection.internal.settings

import android.content.Context
import android.view.View
import com.duckduckgo.anvil.annotations.PriorityKey
import com.duckduckgo.common.ui.view.divider.HorizontalDivider
import com.duckduckgo.common.ui.view.listitem.SectionHeaderListItem
import com.duckduckgo.di.scopes.ActivityScope
import com.duckduckgo.networkprotection.impl.settings.VpnSettingPlugin
import com.duckduckgo.networkprotection.internal.feature.INTERNAL_SETTING_HEADING
import com.duckduckgo.networkprotection.internal.feature.INTERNAL_SETTING_SEPARATOR
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject

@ContributesMultibinding(ActivityScope::class)
@PriorityKey(INTERNAL_SETTING_HEADING)
class VpnInternalSettingHeading @Inject constructor() : VpnSettingPlugin {
    override fun getView(context: Context): View {
        return SectionHeaderListItem(context).apply {
            primaryText = "Employee only settings"
        }
    }
}

@ContributesMultibinding(ActivityScope::class)
@PriorityKey(INTERNAL_SETTING_SEPARATOR)
class VpnInternalSettingSeparator @Inject constructor() : VpnSettingPlugin {
    override fun getView(context: Context): View {
        return HorizontalDivider(context)
    }
}
