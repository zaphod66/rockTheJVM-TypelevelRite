package com.zaphod.jobsboard.domain

object pagination {
  final case class Pagination private (limit: Int, offset: Int)

  object Pagination {
    val defaultPageSize = 20

    def apply(limitF: Option[Int], offsetF: Option[Int]) =
      new Pagination(limitF.getOrElse(defaultPageSize), offsetF.getOrElse(0))

    val default = new Pagination(defaultPageSize, 0)
  }
}
