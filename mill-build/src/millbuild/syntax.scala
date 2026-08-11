package millbuild

import mill.api.*

/** 扩展Task语法 */
extension [T](task: Task[T])
  /** 同步、按顺序执行一个子任务，丢弃返回值，失败时让外层命令失败 */
  inline def runOrFail(evaluator: Evaluator): Unit =
    evaluator
      .execute(Seq(task))
      .values
      .errorOpt
      .foreach(Task.fail)
