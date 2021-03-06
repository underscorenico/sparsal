package algorithms

import algorithms.mp.MatchingPursuit1D
import dictionaries.{Dictionary, Gabor}
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalactic.TolerantNumerics
import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar
import utils._

class MatchingPursuitSuite extends FunSuite  with MockitoSugar {

  test ("Stop condition accuracy achieved should return the acc and residual") {

    val accuracy = mock[Accuracy]
    val dict = mock[Dictionary]
    when(accuracy.estimate(any(), any())).thenReturn(true)
    when(dict.atoms).thenReturn(new Array2DRowRealMatrix(1, 1))

    val mp = MatchingPursuit1D(Vector.empty, dict)
    val result: (List[(Double, Int)], Seq[Double]) = mp.run(accuracy)

    assert(result._1.isEmpty)
    assert(Vector.empty.equals(result._2))
  }

  test ("Stop condition all atoms chosen achieved should return the acc and residual") {

    // One fake atom that is also used as input
    val atom: Array[Double] = Array.fill(10)(5)

    val dict = mock[Dictionary]
    when(dict.atoms).thenReturn(new Array2DRowRealMatrix(atom))

    val mp = MatchingPursuit1D(atom, dict)
    val result: (List[(Double, Int)], Seq[Double]) = mp.run(SNR(20.0))

    // The expected output:
    val expected: List[(Double, Int)] = List((1.0, 0))
    assert(expected.equals(result._1))

    val epsilon = 1e-4f
    implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(epsilon)
    assert(result._2.sum === 0.0)
  }

  test ("MP should converge, input made out of two atoms") {

    // Fake atoms that are also used as input
    val atom1: Array[Double] = Array.fill(10)(5)
    val atom2: Array[Double] = Array.fill(5)(1.5) ++ Array.fill(5)(-1.5)
    val atoms: Array[Array[Double]] = Array(atom1, atom2)
    val input: Array[Double] = atom1.zip(atom2).map { case (x, y) => x + y }

    val dict = mock[Dictionary]
    when(dict.atoms).thenReturn(new Array2DRowRealMatrix(atoms).transpose())

    val mp = MatchingPursuit1D(input, dict)
    val result: (List[(Double, Int)], Seq[Double]) = mp.run(SNR(24.0))

    assert(2.equals(result._1.length))
    // Check that both atoms where chosen from the dictionary
    assert(2.equals(result._1.map(_._2).distinct.length))

    val epsilon = 1e-4f
    implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(epsilon)
    assert(result._2.sum === 0.0)
  }


  test("Sparse decomposition of sine wave") {
    val orig: Seq[Double] = sine(1000, 1, 48000, 64)
    val dict = new Gabor(orig.size)
    val a = MatchingPursuit1D(orig, dict)
    val snr = SNR(4.0)
    val b = a.run(snr)

    assert(b._2.length == orig.length)
  }

  def sine(f: Double, amp: Double, samplef: Double, samples: Int): IndexedSeq[Double] = {
    val wavePeriod = 1 / f
    val samplePeriod = 1 / samplef
    val dTheta = (samplePeriod / wavePeriod) * 2 * math.Pi

    (0 until samples).map(n => math.sin(dTheta * n) * amp)
  }
}
