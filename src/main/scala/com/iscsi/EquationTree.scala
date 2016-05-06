package com.iscsi

import scala.reflect.runtime.{currentMirror => m}
import scala.tools.reflect.ToolBox


object EquationTree extends App {
  // note this will be kept as List[String]
  // scala console invocation:
  //       EquationTree.main(Array(2,3,5,7,11).map(_.toString))
  val givenList = args.toList

  // what we're working with
  val arithOpList = "+-*/".toList.map(_.toString)

  // reflection base
  val tb = m.mkToolBox()

  // generate n - 1 partitions from givenList
  val noParenEqns = (1 until givenList.length).toList.map { x =>
    givenList.splitAt(x)
  }

  // for each expression pair see if paren expansion is necessary
  val parensEqns =
    noParenEqns.flatMap { case (lhs, rhs) =>
      if (lhs.length > 2) {
        for {
          l <- applyParens(parenPosition(lhs.length), lhs)
        } yield (l, rhs)
      } else {
        for {
          r <- applyParens(parenPosition(rhs.length), rhs)
        } yield (lhs, r)
      }
    }

  println(s"found ${parensEqns.length} possible paren equations")
  println(s"found ${noParenEqns.length} possible PEMDOS equations")

  val allEqns = noParenEqns ++ parensEqns

  // use a comprehension over the partitions to apply the combinations
  // of each operator
  val eqns = for {
  // eqPair is a tuple of the left hand side (LHS)/right hand side (RHS)
  // numerical terms
    eqPair <- allEqns
    lhsOps <- genCombinations(arithOpList, eqPair._1.length - 1)
    rhsOps <- genCombinations(arithOpList, eqPair._2.length - 1)
    lhsTerms = eqPair._1
    rhsTerms = eqPair._2
    lhs = interweave(lhsTerms, lhsOps).mkString
    rhs = interweave(rhsTerms, rhsOps).mkString
    lhEval = eval(lhs)
    rhEval = eval(rhs)
    if lhEval == rhEval && lhEval != 0 && rhEval != 0
  } yield s"$lhs=$rhs"

  println(s"found ${eqns.length} possibilities")
  eqns.foreach(println)

  /**
    * All combination with duplicates achieved by replicated input list x length
    *
    * @param opList arithmetic ones
    * @param taken combinations taken
    * @return
    */
  def genCombinations(opList: List[String], taken: Int): List[List[String]] =
    opList.flatMap(x => List.fill(opList.length)(x)).
      combinations(taken).flatMap(_.permutations.toList).toList

/**
  * embed an n - 1 long list of operators between a n long list of numerical terms
  *
  * @param terms numbers
    * @param operators arithmetic
    * @return
    */
  def interweave(terms: List[String], operators: List[String]) =
    terms.zip(operators).flatMap{ case(a,b) =>
        List(a,b)
    } :+ terms.last

  /**
    * use reflection toolbox to parse syntax tree
    * @param expr tree
    * @return
    */
  def eval(expr: String): Int =
    try {
      val tree = tb.parse(expr)
      val obj = tb.eval(tree)
      obj.asInstanceOf[Int]
    } catch {
      case e: Exception => 0    // catch / by zero
    }

  /**
    * figure out where to place parens (NOTE: only invoked on 3 or more terms
    *   we drop parens around 1st and nth terms as redudant
    *
    * @param n number of terms in an expression (LHS/RHS)
    * @return
    */
  def parenPosition(n: Int): List[List[(Int,String)]] =
  // iterate over a range
    (1 to n).toList.
    // taking 2 at a time to represent where parens would go
      combinations(2).
    // and drop outermost parens
      filter(x => x != List(1, n)).toList.map {x =>
      // simply processing by ensuring parens match
      List((x.head, "("), (x(1), ")"))
    }

  def paren(term: String, tuple: (Int, String)): String =
      tuple._2 match {
        case "(" => s"($term"
        case ")" => s"$term)"
      }

  def applyParens2(pair: List[(Int, String)], termList: List[String]): List[String] = {
    val untouchedIndices = (1 to termList.length).toList diff List(pair.head._1) diff List(pair.last._1)
    val changedMap = Map(pair.head._1 -> paren(termList(pair.head._1 - 1), pair.head)) ++
                     Map(pair.last._1 -> paren(termList(pair.last._1 - 1), pair.last))
    val unchangedMap =
      untouchedIndices.map { ndx =>
        ndx -> termList(ndx - 1)
      }.toMap
    val allTerms = changedMap ++ unchangedMap

    allTerms.keys.toList.sorted.map { x =>
    allTerms(x)}
  }

  /**
    * given a set of positions specifying where parens go apply them to a term list
    * List(List(1,2),List(1,3)) applied to List["2","3","5"] would generate
    * List(List("(2", "3)", "5"), List("2", "(3", "5)"))
    *
    * @param parenList results of parenPosition()
    * @param termList must be >= 3 terms
    * @return
    */
  def applyParens(parenList: List[List[(Int, String)]], termList: List[String]): List[List[String]] =
    parenList.map { pairs => applyParens2(pairs, termList)}
}

