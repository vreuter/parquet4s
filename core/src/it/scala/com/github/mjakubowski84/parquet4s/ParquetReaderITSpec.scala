package com.github.mjakubowski84.parquet4s

import org.joda.time.DateTimeZone
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.reflect.runtime.universe.TypeTag


object ParquetReaderITSpec {

  case class RowWithPrimitives(b: Boolean, i: Int, l: Long, f: Float, d: Double, s: String)
  case class RowWithTime(timestamp: java.sql.Timestamp, date: java.sql.Date)
  case class RowWithOptions(opt1: Option[String], opt2: Option[Int])
  case class RowWithCols(list: List[String], nestedCols: List[List[Float]], set: Set[Int], seq: Seq[Int])
  case class RowWithArray(array: Array[Int])
  case class RowWithMap(map: Map[String, Int])
  case class NestedClass(n: Int)
  case class RowWithNestedClass(nested: NestedClass)
  case class RowWithOptionalNestedClass(nested: Option[NestedClass])
  case class RowWithListOfNestedClass(nested: List[NestedClass])
  case class RowWithSetOfNestedClass(nested: Set[NestedClass])
  case class RowWithArrayOfNestedClass(nested: Array[NestedClass])
  case class RowWithSequenceOfNestedClass(nested: Seq[NestedClass])
  case class RowWithVectorOfNestedClass(nested: Vector[NestedClass])
  case class RowWithMapOfNestedClassAsValue(nested: Map[String, NestedClass])
  case class RowWithListOfOptions(list: List[Option[Int]])
  case class RowWithMapOfOptions(map: Map[String, Option[Int]])

}

class ParquetReaderITSpec
  extends FlatSpec
    with Matchers
    with BeforeAndAfter
    with SparkHelper {

  import ParquetReaderITSpec._

  before {
    clearTemp()
  }

  class Fixture[Row <: Product : TypeTag : ParquetReader](rows: Seq[Row]) {
    private val reader = ParquetReader.read[Row](tempPathString)
    writeToTemp(rows)
    try {
      reader should contain theSameElementsAs rows
    } finally {
      reader.close()
    }
  }

  class FixtureWithMapping[Row <: Product : TypeTag : ParquetRecordDecoder, Mapped](rows: Seq[Row], mapping: Row => Mapped) {
    private val reader = ParquetReader[Row](tempPathString)
    writeToTemp(rows)
    try {
      reader.map(mapping) should contain theSameElementsAs rows.map(mapping)
    } finally {
      reader.close()
    }
  }

  "Parquet reader" should "be able to read data with primitive types" in new Fixture(Seq(
    RowWithPrimitives(b = true, i = 1, l = 100, f = 0.1f, d = 10.1, s = "some string"),
    RowWithPrimitives(b = false, i = -1, l = -100, f = -0.1f, d = -10.1, s = "")
  ))

  it should "be able to read data with time types" in new Fixture(Seq(
    RowWithTime(
      timestamp = new java.sql.Timestamp(new org.joda.time.DateTime(2018, 1, 1, 12, 0, 59, DateTimeZone.getDefault).getMillis),
      date = new java.sql.Date(new org.joda.time.DateTime(2018, 1, 1, 0, 0, 0, DateTimeZone.getDefault).getMillis)
    )
  ))

  it should "be able to read data with optional types" in new Fixture(Seq(
    RowWithOptions(opt1 = Some("I am not empty"), opt2 = Some(123)),
    RowWithOptions(opt1 = None, opt2 = None)
  ))

  it should "be able to read data with collection types" in new Fixture(Seq(
    RowWithCols(List.empty, List.empty, Set.empty, Seq.empty),
    RowWithCols(List("a", "b", "c"), List(List(1f, 2f, 3f), List(4f, 5f, 6f)), Set(1, 2, 3), Seq(1, 2, 3))
  ))

  it should "be able to read data with array types" in new FixtureWithMapping[RowWithArray, List[Int]](
    rows = Seq(
      RowWithArray(Array.emptyIntArray),
      RowWithArray(Array(1, 2, 3))
    ),
    mapping = _.array.toList
  )

  it should "be able to read data with map types" in new Fixture(Seq(
    RowWithMap(Map("a" -> 1, "b" -> 2, "c" -> 3)),
    RowWithMap(Map.empty)
  ))

  it should "be able to read data with nested class" in new Fixture(Seq(
    RowWithNestedClass(NestedClass(1))
  ))

  it should "be able to read data with optional nested class" in new Fixture(Seq(
    RowWithOptionalNestedClass(None),
    RowWithOptionalNestedClass(Some(NestedClass(1)))
  ))

  it should "be able to read data with list of nested class" in new Fixture(Seq(
    RowWithListOfNestedClass(List(NestedClass(1), NestedClass(2), NestedClass(3))),
    RowWithListOfNestedClass(List.empty)
  ))

  it should "be able to read data with set of nested class" in new Fixture(Seq(
    RowWithSetOfNestedClass(Set(NestedClass(1), NestedClass(2), NestedClass(3))),
    RowWithSetOfNestedClass(Set.empty)
  ))

  it should "be able to read data with array of nested class" in new FixtureWithMapping[RowWithArrayOfNestedClass, List[NestedClass]](
    rows = Seq(
      RowWithArrayOfNestedClass(Array(NestedClass(1), NestedClass(2), NestedClass(3))),
      RowWithArrayOfNestedClass(Array.empty)
    ),
    mapping = _.nested.toList
  )

  it should "be able to read data with sequence of nested class" in new Fixture(Seq(
    RowWithSequenceOfNestedClass(Seq(NestedClass(1), NestedClass(2), NestedClass(3))),
    RowWithSequenceOfNestedClass(Seq.empty)
  ))

  it should "be able to read data with vector of nested class" in new Fixture(Seq(
    RowWithVectorOfNestedClass(Vector(NestedClass(1), NestedClass(2), NestedClass(3))),
    RowWithVectorOfNestedClass(Vector.empty)
  ))

  it should "be able to read data with map that contains nested class as a value" in new Fixture(Seq(
    RowWithMapOfNestedClassAsValue(Map("1" -> NestedClass(1), "2" -> NestedClass(2), "3" -> NestedClass(3))),
    RowWithMapOfNestedClassAsValue(Map.empty)
  ))

  it should "be able to read collection of options" in new Fixture(Seq(
    RowWithListOfOptions(List(None, Some(1), None))
  ))

  it should "be able to read map of options" in new Fixture(Seq(
    RowWithMapOfOptions(Map("1" -> None, "2" -> Some(1), "3" -> None))
  ))

}