package com.ecjtu.heaven.ui.adapter

import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.Target
import com.ecjtu.heaven.R
import com.ecjtu.heaven.ui.activity.FullScreenImageActivity
import com.ecjtu.netcore.jsoup.PageDetailSoup
import com.ecjtu.netcore.jsoup.SoupFactory
import com.ecjtu.netcore.model.PageDetailModel
import com.ecjtu.netcore.network.AsyncNetwork
import com.ecjtu.netcore.network.IRequestCallback
import java.net.HttpURLConnection

/**
 * Created by Ethan_Xiang on 2017/9/11.
 */
class PageDetailAdapter(var pageModel: PageDetailModel) : RecyclerView.Adapter<PageDetailAdapter.VH>(), RequestListener<Bitmap>, View.OnClickListener {
    private val mListHeight = ArrayList<Int>()

    override fun getItemCount(): Int {
        return pageModel.maxLen
    }

    override fun onBindViewHolder(holder: VH?, position: Int) {
        val context = holder?.itemView?.context
        val params = holder?.itemView?.layoutParams
        if (getHeight(position) != 0) {
            params?.height = getHeight(position)/*mLastHeight*/ // 防止上滑时 出现跳动的情况
        } else {
            val next = getHeight(position + 1)
            val last = getHeight(position - 1)
            if (next != 0) {
                params?.height = next
            } else if (last != 0) {
                params?.height = last
            } else {
                params?.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, context?.resources?.displayMetrics).toInt()
            }
        }

        val imageView = holder?.mImageView
        val options = RequestOptions()
        options.centerCrop()
        val url = String.format(pageModel.imgUrl, position + 1)
        val builder = LazyHeaders.Builder().addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Mobile Safari/537.36")
                .addHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
                .addHeader("Host", "i.meizitu.net")
                .addHeader("Proxy-Connection", "keep-alive")
                .addHeader("Referer", "http://m.mzitu.com/")
                .addHeader("Base-Url", pageModel.baseUrl)
                .addHeader("Base-Position", position.toString())
        val glideUrl = GlideUrl(url, builder.build())
        url.let {
            imageView?.setTag(R.id.extra_tag, position)
            holder?.itemView?.setOnClickListener(this)
            Glide.with(context).asBitmap().load(glideUrl).listener(this).apply(options).into(imageView)
        }

        holder?.itemView?.setTag(R.id.extra_tag, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): VH {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.layout_card_view, parent, false)
        return VH(v)
    }

    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
        val request = AsyncNetwork()
        val header = model as GlideUrl
        val url = header.headers["Base-Url"] ?: "" + "/" + header.headers["Base-Position"]
        request.request(url, null)
        request.setRequestCallback(object : IRequestCallback {
            override fun onSuccess(httpURLConnection: HttpURLConnection?, response: String) {
                val ret = SoupFactory.parseHtml(PageDetailSoup::class.java, response, url)
                val model = ret.get("origin_img")
                if (model != null) {
                    if (target is BitmapImageViewTarget){
                        target.view.post{
                            val builder = LazyHeaders.Builder().addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Mobile Safari/537.36")
                                    .addHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                                    .addHeader("Accept-Encoding", "gzip, deflate")
                                    .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
                                    .addHeader("Host", "i.meizitu.net")
                                    .addHeader("Proxy-Connection", "keep-alive")
                                    .addHeader("Referer", "http://m.mzitu.com/")
                            val glideUrl = GlideUrl(url, builder.build())
                            Glide.with(target.view.context).asBitmap().load(glideUrl).listener(this@PageDetailAdapter).
                                    into(target.view)
                        }
                    }
                }
            }
        })
        return false
    }

    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
        if (target is BitmapImageViewTarget) {
            val parent = target.view.parent?.parent
            val layoutParams = (parent as View).layoutParams
            var height = resource?.height ?: LinearLayout.LayoutParams.WRAP_CONTENT
            height += parent.findViewById(R.id.title).height
            if (layoutParams.height != height) {
                layoutParams.height = height
            }
            val position = target.view.getTag(R.id.extra_tag) as Int
            target.view.setImageBitmap(resource)
            setHeight(position, height)
        }
        return true
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    private fun getHeight(position: Int): Int {
        if (position >= mListHeight.size || position < 0) {
            return 0
        }
        return mListHeight[position]
    }

    private fun setHeight(position: Int, height: Int) {
        if (position >= mListHeight.size) {
            val diff = position - mListHeight.size + 1
            mListHeight.addAll(Array<Int>(diff, { 0 }))
        }
        mListHeight.set(position, height)
    }

    override fun onClick(v: View?) {
        val position = v?.getTag(R.id.extra_tag)
        if (position != null) {
            val intent = FullScreenImageActivity.newInstance(v.context, String.format(pageModel.imgUrl, position as Int + 1))
            v.context.startActivity(intent)
        }
    }


    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mImageView = itemView.findViewById(R.id.image) as ImageView
        val mBottom = itemView.findViewById(R.id.bottom) as View

        init {
            mImageView.adjustViewBounds = true
            mBottom.visibility = View.GONE
        }
    }
}