package search.common.tool.ictclas

import com.sun.jna.{Native, Library}

/**
  * Created by soledede.weng on 2016/9/2.
  */
class IctclasSegmentation extends Segmentation {
  private var nlpir: NLPIR = null
  nlpir = NLPIR.INSTANCE
  nlpir.NLPIR_Init("", 1, "")


  override def addUserWord(userWord: String): Unit = {
    nlpir.NLPIR_AddUserWord(userWord + " " + USER_DEFINE)
  }

  override def parse(paragraph: String, showNature: Boolean): String = {
    nlpir.NLPIR_ParagraphProcess(paragraph, if (showNature) 1 else 0);
  }

  @throws(classOf[Throwable])
  protected override def finalize {
    nlpir.NLPIR_Exit
    super.finalize
  }
}

object NLPIR {
  val INSTANCE: NLPIR = Native.loadLibrary("NLPIR", classOf[NLPIR]).asInstanceOf[NLPIR]
}

trait NLPIR extends Library {

  /**
    * 组件初始化
    *
    * @param sDataPath    Data文件夹的父目录，如果为空字符串（即：""），那么，程序自动从项目的根目录中寻找
    * @param encoding     编码格式，具体的编码对照如下： 0：GBK；1：UTF8；2：BIG5；3：GBK，里面包含繁体字
    * @param sLicenceCode 授权码，为空字符串（即：""）就可以了
    * @return true：初始化成功；false：初始化失败
    */
  def NLPIR_Init(sDataPath: String, encoding: Int, sLicenceCode: String): Boolean

  /**
    * 分词
    *
    * @param sParagraph 文本内容
    * @param bPOSTagged 1：显示词性；0：不显示词性
    * @return 分词结果
    */
  def NLPIR_ParagraphProcess(sParagraph: String, bPOSTagged: Int): String

  /**
    * 添加用户自定义词
    *
    * @param userWord 用户词 格式：单词+空格+词性，例如：你好 v
    * @return 1：内存中不存在；2：内存中已存在 备注：保存到内存中，下次初始化后失效，需要用save保存到文件中
    */
  def NLPIR_AddUserWord(userWord: String): Int

  /**
    * 退出，释放资源
    *
    * @return 成功或失败
    */
  def NLPIR_Exit: Boolean
}