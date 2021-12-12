package org.laolittle.plugin.joinorquit.util

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.File
import java.io.IOException
import javax.imageio.*
import javax.imageio.metadata.IIOInvalidTreeException
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream
import javax.imageio.stream.ImageOutputStream

@Suppress("KotlinConstantConditions", "SameParameterValue")
class GifEncoder private constructor(outputStream: ImageOutputStream, imageType: Int, delay: Int, loop: Boolean) {
    private val writer: ImageWriter = ImageIO.getImageWritersBySuffix("gif").next()
    private val params: ImageWriteParam = writer.defaultWriteParam
    private val metadata: IIOMetadata

    @Throws(IIOInvalidTreeException::class)
    private fun configureRootMetadata(delay: Int, loop: Boolean) {
        val metaFormatName = metadata.nativeMetadataFormatName
        val root = metadata.getAsTree(metaFormatName) as IIOMetadataNode
        val graphicsControlExtensionNode = getNode(root, "GraphicControlExtension")
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none")
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE")
        graphicsControlExtensionNode.setAttribute("delayTime", (delay / 10).toString())
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0")
        val appExtensionsNode = getNode(root, "ApplicationExtensions")
        val child = IIOMetadataNode("ApplicationExtension")
        child.setAttribute("applicationID", "NETSCAPE")
        child.setAttribute("authenticationCode", "2.0")
        val loopContinuously = if (loop) 0 else 1
        child.userObject =
            byteArrayOf(0x1, (loopContinuously and 0xFF).toByte(), (loopContinuously shr 8 and 0xFF).toByte())
        appExtensionsNode.appendChild(child)
        metadata.setFromTree(metaFormatName, root)
    }

    @Throws(IOException::class)
    fun writeToSequence(img: RenderedImage?) {
        writer.writeToSequence(IIOImage(img, null, metadata), params)
    }

    @Throws(IOException::class)
    fun close() {
        writer.endWriteSequence()
    }

    companion object {

        private fun getNode(rootNode: IIOMetadataNode, nodeName: String): IIOMetadataNode {
            val nNodes = rootNode.length
            for (i in 0 until nNodes) {
                if (rootNode.item(i).nodeName.equals(nodeName, ignoreCase = true)) {
                    return rootNode.item(i) as IIOMetadataNode
                }
            }
            val node = IIOMetadataNode(nodeName)
            rootNode.appendChild(node)
            return node
        }

        private fun convert(
            images: Array<BufferedImage>,
            outputStream: ImageOutputStream,
            delay: Int,
            loop: Boolean,
            width: Int?,
            height: Int?
        ) {
            //图像类型
            val imageType = images[0].type
            //缩放参数
            val sx = if (width == null) 1.0 else width.toDouble() / images[0].width
            val sy = if (height == null) 1.0 else height.toDouble() / images[0].height
            val op = AffineTransformOp(AffineTransform.getScaleInstance(sx, sy), null)
            try {
                val gif = GifEncoder(outputStream, imageType, delay, loop)
                for (image in images) {
                    gif.writeToSequence(op.filter(image, null))
                }
                gif.close()
                outputStream.close()
            } catch (e: Exception) {
                throw RuntimeException("GIF编码出错", e)
            }
        }

        /*
                private fun convert(
                    imagePaths: Array<String>,
                    gifPath: String,
                    delay: Int,
                    loop: Boolean,
                    width: Int?,
                    height: Int?
                ) {
                    try {
                        val images = arrayOfNulls<BufferedImage>(imagePaths.size)
                        for (i in imagePaths.indices) {
                            images[i] = ImageIO.read(File(imagePaths[i]))
                        }
                        val fileImageOutputStream = FileImageOutputStream(File(gifPath))
                        convert(images, fileImageOutputStream, delay, loop, width, height)
                    } catch (e: Exception) {
                        throw RuntimeException("GIF convert error", e)
                    }
                }
        */
        private fun convert(
            images: Array<BufferedImage>,
            gifPath: String,
            delay: Int,
            loop: Boolean,
            width: Int?,
            height: Int?
        ) {
            val fileImageOutputStream = FileImageOutputStream(File(gifPath))
            convert(images, fileImageOutputStream, delay, loop, width, height)
        }
/*
        fun convert(imagePaths: Array<String>, gifPath: String, delay: Int, loop: Boolean) {
            convert(imagePaths, gifPath, delay, loop, null, null)
        }

 */

        /**
         * convert Jpeg to Gif
         * PNG is not support (For now)
         *
         * 将Jpeg转换为Gif图片
         * 暂不支持PNG图片
         * @param images: 传入的图片，有顺序
         * @param gifPath: 输出路径
         * @param delay: 每张图片的切换间隔
         * @param loop: 是否循环播放
         *
         * */

        fun convert(images: Array<BufferedImage>, gifPath: String, delay: Int, loop: Boolean) {
            convert(images, gifPath, delay, loop, null, null)
        }
    }

    init {
        metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromBufferedImageType(imageType), params)
        //配置元数据
        configureRootMetadata(delay, loop)
        //设置输出流
        writer.output = outputStream
        writer.prepareWriteSequence(null)
    }
}