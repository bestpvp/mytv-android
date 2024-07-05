package top.yogiczy.mytv.data.repositories.iptv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import top.yogiczy.mytv.data.entities.Iptv
import top.yogiczy.mytv.data.entities.IptvGroup
import top.yogiczy.mytv.data.entities.IptvGroupList
import top.yogiczy.mytv.data.entities.IptvGroupList.Companion.iptvGroupIdx
import top.yogiczy.mytv.data.entities.IptvGroupList.Companion.iptvIdx
import top.yogiczy.mytv.data.entities.IptvList
import top.yogiczy.mytv.data.repositories.FileCacheRepository
import top.yogiczy.mytv.data.repositories.iptv.parser.IptvParser
import top.yogiczy.mytv.data.utils.Constants
import top.yogiczy.mytv.utils.Logger

/**
 * 直播源获取
 */
class IptvRepository : FileCacheRepository("iptv.txt") {
    private val log = Logger.create(javaClass.simpleName)

    /**
     * 获取远程直播源数据
     */
    private suspend fun fetchSource(sourceUrl: String) = withContext(Dispatchers.IO) {
        log.d("获取远程直播源: $sourceUrl")

        val client = OkHttpClient()
        val request = Request.Builder().url(sourceUrl).build()

        try {
            with(client.newCall(request).execute()) {
                if (!isSuccessful) {
                    throw Exception("获取远程直播源失败: $code")
                }

                return@with body!!.string()
            }
        } catch (ex: Exception) {
            log.e("获取远程直播源失败", ex)
            throw Exception("获取远程直播源失败，请检查网络连接", ex)
        }
    }

    /**
     * 简化规则
     */
    private fun simplifyTest(group: IptvGroup, iptv: Iptv): Boolean {
        return iptv.name.lowercase().startsWith("cctv") || iptv.name.endsWith("卫视")
    }

    /**
     * 获取直播源分组列表
     */
    suspend fun getIptvGroupList(
        sourceUrl: String,
        cacheTime: Long,
        simplify: Boolean = false,
    ): IptvGroupList {
        try {
            val sourceData = getOrRefresh(cacheTime) {
                fetchSource(sourceUrl)
            }

            val parser = IptvParser.instances.first { it.isSupport(sourceUrl, sourceData) }
            val groupList = parser.parse(sourceData).toMutableList()

            log.i("解析直播源完成：${groupList.size}个分组，${groupList.flatMap { it.iptvList }.size}个频道")

            // 动态添加新的频道
            val newChannel = Iptv(
                name = Constants.APP_TITLE,
                channelName = Constants.APP_TITLE,
                urlList = listOf(
                    Constants.INSERT_SOURCE_URL,
                    // "https://mirror.ghproxy.com/https://raw.githubusercontent.com/bestpvp/config/main/cut/test.m3u",
                )
            )

            // 假设要添加到第一个组，如果不存在组则创建一个新的组
            if (groupList.isNotEmpty()) {
                val firstGroup = groupList.first()
                val updatedIptvList = mutableListOf(newChannel).apply { addAll(firstGroup.iptvList) }
                groupList[0] = firstGroup.copy(iptvList = IptvList(updatedIptvList))
            }

            if (simplify) {
                return IptvGroupList(groupList.map { group ->
                    IptvGroup(
                        name = group.name, iptvList = IptvList(group.iptvList.filter { iptv ->
                            simplifyTest(group, iptv)
                        })
                    )
                }.filter { it.iptvList.isNotEmpty() })
            }
            // log.i("==== $groupList")
            return IptvGroupList(groupList)
        } catch (ex: Exception) {
            log.e("获取直播源失败", ex)
            throw Exception(ex)
        }
    }
}