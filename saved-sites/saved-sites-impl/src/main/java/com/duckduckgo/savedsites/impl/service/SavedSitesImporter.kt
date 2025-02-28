/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.savedsites.impl.service

import android.content.ContentResolver
import android.net.Uri
import com.duckduckgo.common.utils.formatters.time.DatabaseDateFormatter
import com.duckduckgo.savedsites.api.SavedSitesRepository
import com.duckduckgo.savedsites.api.models.SavedSite
import com.duckduckgo.savedsites.api.models.SavedSitesNames
import com.duckduckgo.savedsites.api.service.ImportSavedSitesResult
import com.duckduckgo.savedsites.api.service.SavedSitesImporter
import com.duckduckgo.savedsites.store.Entity
import com.duckduckgo.savedsites.store.EntityType.BOOKMARK
import com.duckduckgo.savedsites.store.Relation
import com.duckduckgo.savedsites.store.SavedSitesEntitiesDao
import com.duckduckgo.savedsites.store.SavedSitesRelationsDao
import org.jsoup.Jsoup

/*
 * Copyright (c) 2021 DuckDuckGo
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

class RealSavedSitesImporter(
    private val contentResolver: ContentResolver,
    private val savedSitesEntitiesDao: SavedSitesEntitiesDao,
    private val savedSitesRelationsDao: SavedSitesRelationsDao,
    private val savedSitesRepository: SavedSitesRepository,
    private val savedSitesParser: SavedSitesParser,
) : SavedSitesImporter {

    companion object {
        private const val BASE_URI = "duckduckgo.com"
        private const val IMPORT_BATCH_SIZE = 200
    }

    override suspend fun import(uri: Uri): ImportSavedSitesResult {
        return try {
            val savedSites = contentResolver.openInputStream(uri).use { stream ->
                val document = Jsoup.parse(stream, Charsets.UTF_8.name(), BASE_URI)
                savedSitesParser.parseHtml(document, savedSitesRepository)
            }

            savedSites.filterIsInstance<SavedSite.Bookmark>().map { bookmark ->
                Pair(
                    Relation(folderId = bookmark.parentId, entityId = bookmark.id),
                    Entity(bookmark.id, title = bookmark.title, url = bookmark.url, type = BOOKMARK),
                )
            }.also { pairs ->
                pairs.asSequence().chunked(IMPORT_BATCH_SIZE).forEach { chunk ->
                    savedSitesRelationsDao.insertList(chunk.map { it.first })
                    savedSitesEntitiesDao.insertList(chunk.map { it.second })
                }
            }

            savedSites.filterIsInstance<SavedSite.Favorite>().filter { it.url.isNotEmpty() }.map { favorite ->
                Pair(
                    Relation(folderId = SavedSitesNames.FAVORITES_ROOT, entityId = favorite.id),
                    Entity(favorite.id, title = favorite.title, url = favorite.url, type = BOOKMARK),
                )
            }.also { pairs ->
                pairs.asSequence().chunked(IMPORT_BATCH_SIZE).forEach { chunk ->
                    savedSitesRelationsDao.insertList(chunk.map { it.first })
                    savedSitesEntitiesDao.insertList(chunk.map { it.second })
                }
            }

            savedSites.filterIsInstance<SavedSite.Favorite>().filter { it.url.isNotEmpty() }.map { favorite ->
                Pair(
                    Relation(folderId = SavedSitesNames.BOOKMARKS_ROOT, entityId = favorite.id),
                    Entity(favorite.id, title = favorite.title, url = favorite.url, type = BOOKMARK),
                )
            }.also { pairs ->
                pairs.asSequence().chunked(IMPORT_BATCH_SIZE).forEach { chunk ->
                    savedSitesRelationsDao.insertList(chunk.map { it.first })
                    savedSitesEntitiesDao.insertList(chunk.map { it.second })
                }
            }

            savedSitesEntitiesDao.updateModified(SavedSitesNames.BOOKMARKS_ROOT, DatabaseDateFormatter.iso8601())
            if (savedSites.filterIsInstance<SavedSite.Favorite>().filter { it.url.isNotEmpty() }.isNotEmpty()) {
                savedSitesEntitiesDao.updateModified(SavedSitesNames.FAVORITES_ROOT, DatabaseDateFormatter.iso8601())
            }

            ImportSavedSitesResult.Success(savedSites)
        } catch (exception: Exception) {
            ImportSavedSitesResult.Error(exception)
        }
    }
}
