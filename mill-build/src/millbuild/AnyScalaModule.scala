package millbuild

import com.goyeau.mill.scalafix.ScalafixModule
import mill.api.*
import mill.contrib.scoverage.ScoverageModule
import mill.scalalib.*
import mill.scalalib.scalafmt.ScalafmtModule

import java.awt.Desktop

/** Scala模块基础 */
trait AnyScalaModule extends ScalaModule with ScalafmtModule with ScoverageModule with ScalafixModule:
  override def jvmVersion       = Versions.jvm
  override def scalaVersion     = Versions.scala
  override def scalacOptions    = Commons.scalaCompilerOptions
  override def scoverageVersion = Versions.scoverage

  /** 清洁并格式化代码 */
  def lintFmt(evaluator: Evaluator, args: String*): Task.Command[Unit] = Task.Command(exclusive = true):
    // 执行Scalafix
    this.fix(args*).runOrFail(evaluator)
    // 执行Scalafmt
    this.reformat().runOrFail(evaluator)

  /** 测试并报告覆盖率，返回报告数据文件夹 */
  def testCo(evaluator: Evaluator, args: String*): Task.Command[PathRef] = Task.Command(exclusive = true):
    // 执行默认测试任务
    assert(test.defaultTask() == "testForked")
    test.testForked(args*).runOrFail(evaluator)
    // 执行Scoverage报告任务
    this.scoverage.htmlReport().runOrFail(evaluator)
    // 获取报告文件
    val dataDir = this.scoverage.data().path
    assert(os.isDir(dataDir))
    val htmlReportDir = dataDir / os.up / "htmlReport.dest"
    assert(os.isDir(htmlReportDir))
    val indexHtml = htmlReportDir / "index.html"
    assert(os.isFile(indexHtml))
    // 如果支持，使用浏览器打开
    if Desktop.isDesktopSupported then Desktop.getDesktop.browse(indexHtml.toURI)
    // 返回报告数据
    PathRef(dataDir)

  object test extends ScoverageTests with TestModule.Munit:
    override def munitVersion = Versions.munit
