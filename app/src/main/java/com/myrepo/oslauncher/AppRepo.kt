package com.myrepo.oslauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore

data class AppItem(
    val label: String,
    val pkg: String,
    val component: ComponentName,
    val icon: Drawable
)

object AppRepo {
    fun load(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0).map { ri ->
            AppItem(
                ri.loadLabel(pm).toString(),
                ri.activityInfo.packageName,
                ComponentName(ri.activityInfo.packageName, ri.activityInfo.name),
                ri.loadIcon(pm)
            )
        }.sortedBy { it.label.lowercase() }
    }

    /** Телефон, сообщения, браузер, камера — если такие приложения есть */
    fun defaultDock(ctx: Context, apps: List<AppItem>, max: Int): List<String> {
        val pm = ctx.packageManager
        val known = apps.map { it.pkg }.toSet()
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        )
        val out = LinkedHashSet<String>()
        for (i in intents) {
            val p = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            if (p != null && p in known) out.add(p)
        }
        for (a in apps) {
            if (out.size >= max) break
            out.add(a.pkg)
        }
        return out.take(max)
    }
}
