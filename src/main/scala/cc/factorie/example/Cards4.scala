package cc.factorie.example
import cc.factorie._
import scala.collection.mutable.{ArrayBuffer,HashMap}

object PragBot4 {

  object SuitDomain extends CategoricalDomain(List("H", "D", "C", "S"))
  class Suit(name:String) extends CategoricalVariable(name) {
    def domain = SuitDomain
  }

  class Card(val n:Int, val suit:Suit#Value, location:Location) extends RefVariable[Location](location) with IterableSettings with Ordered[Card] {
    def compare(other:Card) = { val d = (other.suit.intValue*13 + other.n) - (this.suit.intValue*13 + this.n); if (d < 0.0) 1 else if (d > 0.0) -1 else 0 }
    def settings = new LocationIterator(this) with SettingIterator
    override def toString = n.toString + suit
  }

  class Location(val name:String) {
    def cardsHere = allCards.filter(_.value == this)
    val wins = new BooleanVar { def value: Value = hasSuitRun(cardsHere, 3) }
  }

  def main(args:Array[String]): Unit = {
    val jane = new Location("jane")
    val floor = new Location("floor")
    val locations = List(floor, jane)
    //val suits = List("H", "D", "C", "S").map(new Suit(_))
    val cards = SuitDomain.values.flatMap(suit => for (i <- 0 until 13) yield new Card(i, suit, floor))
    val card = new HashMap[String,Card] ++= cards.map(c => (c.toString, c))
    allLocations = locations
    allCards = cards

    val model = new Model(
      new TemplateWithStatistics1[Card] {
        def score(s:Stat) = if (s._1 == jane) -1.0 else 0.0
      }
    )

    val inferencer = new SamplingInferencer[BooleanVar,Card](new VariableSettingsSampler[Card](model) {
      override def postProcessHook(c:Card, d:DiffList): Unit = if (iterationCount % 500 == 0) println(jane.cardsHere.sorted.toString+"  wins="+jane.wins.value)
    })
    inferencer.burnIn = 1; inferencer.iterations = 10000; inferencer.thinning = 100
    val marginals = inferencer.infer(List(jane.wins), cards)
    println ("\np(jane.wins)="+marginals(jane.wins).pr(1))
  }








  var allLocations: Seq[Location] = null
  var allCards: Seq[Card] = null

  class LocationIterator(card:Card) extends SettingIterator {
    var i = -1
    def reset = i = -1
    def hasNext = i < allLocations.length-1
    def next(d:DiffList): DiffList = {
      i += 1
      val d = newDiffList
      card.set(allLocations(i))(d)
      d
    }
  }

  def isRun(s:Seq[Card]): Boolean = { for (i <- 0 until s.length - 1) if (s(i).n != s(i+1).n-1) return false; return true }
  def hasSuitRun(cards:Seq[Card], runLength:Int): Boolean = {
    val rl = runLength + 1
    val bySuit = cards.groupBy(_.suit).values
    if (bySuit.exists(cards => { val sortedCards = cards.sorted; Range(0,sortedCards.length-rl).map(i => sortedCards.slice(i, i+rl)).exists(isRun(_)) })) true else false
  }


}
    //card("2H") := jane
    //card("3H") := jane
    //card("4H") := jane
    //println("jane.wins="+jane.wins.value)
