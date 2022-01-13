;;
;; Copyright © 2022 Sam Ritchie.
;; This work is based on the Scmutils system of MIT/GNU Scheme:
;; Copyright © 2002 Massachusetts Institute of Technology
;;
;; This is free software;  you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation; either version 3 of the License, or (at
;; your option) any later version.
;;
;; This software is distributed in the hope that it will be useful, but
;; WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with this code; if not, see <http://www.gnu.org/licenses/>.
;;

(ns sicmutils.special.factorial
  "Namespace holding implementations of variations on the factorial function."
  (:require [sicmutils.generic :as g]
            [sicmutils.numbers]
            [sicmutils.util :as u]
            [sicmutils.util.def :refer [defgeneric]
             #?@(:cljs [:include-macros true])]
            [sicmutils.value :as v]))

;; TODO factorial should be symbolic too, and render as !... make a ticket!

(defn factorial
  "Returns the factorial of `n`, ie, the product of 1 to `n` (inclusive).

  [[factorial]] will return a platform-specific [[sicmutils.util/bigint]] given
  some `n` that causes integer overflow."
  [n]
  {:pre [(v/native-integral? n)
         (>= n 0)]}
  (let [elems (range 1 (inc n))]
    #?(:clj
       (apply *' elems)
       :cljs
       (if (<= n 20)
         (apply * elems)
         (transduce (map u/bigint) g/* elems)))))

;; ## Falling and Rising Factorials
;;
;; TODO fill these in https://en.wikipedia.org/wiki/Falling_and_rising_factorials

;; NOTE from wiki: The rising and falling factorials are well defined in any
;; unital ring, and therefore x can be taken to be, for example, a complex
;; number, including negative integers, or a polynomial with complex
;; coefficients, or any complex-valued function.

(declare rising-factorial)

#_(comment
    ;; here is another impl for negative that we can test against.
    (g/invert
     (transduce (comp
                 (map #(g/add x (inc %)))
                 #?(:cljs (map u/bigint)))
                g/*
                (range (- n)))))

;; TODO coefficients of expansions are stirling numbers of the first kind, see
;; https://en.wikipedia.org/wiki/Falling_and_rising_factorials#cite_note-10

;; Cool, this works!
;;
;; TODO see https://proofwiki.org/wiki/Properties_of_Falling_Factorial tests for
;; falling

(defgeneric falling-factorial 2
  "Falling factorial docstring.")

(def ^{:doc "Alias for [[falling-factorial]]."}
  factorial-power
  falling-factorial)

(defmethod falling-factorial :default [x n]
  {:pre [(v/native-integral? n)]}
  (cond (zero? n) 1
        (neg? n)
        (let [denom (rising-factorial (g/add x 1) (- n))]
          (if (v/zero? denom)
            ##Inf
            (g/invert denom)))

        :else
        (transduce (comp
                    (map #(g/add x (- %)))
                    #?(:cljs (map u/bigint)))
                   g/*
                   (range n))))

(defmethod falling-factorial [::v/native-integral ::v/native-integral] [x n]
  (cond (zero? n) 1
        (neg? n)
        (let [denom (rising-factorial (inc x) (- n))]
          (if (zero? denom)
            ##Inf
            (/ 1 denom)))

        :else
        (let [elems (range x (- x n) -1)]
          #?(:clj
             (apply *' elems)
             :cljs
             (transduce (map u/bigint) * elems)))))

(defgeneric rising-factorial 2
  "Rising factorial docstring.")

(def ^{:doc "Alias for [[falling-factorial]]."}
  pochhammer
  rising-factorial)

(defmethod rising-factorial :default [x n]
  {:pre [(v/native-integral? n)]}
  (cond (zero? n) 1
        (neg? n)
        (let [denom (falling-factorial (g/sub x 1) (- n))]
          (if (v/zero? denom)
            ##Inf
            (g/invert denom)))

        :else
        (transduce (comp
                    (map #(g/add x %))
                    #?(:cljs (map u/bigint)))
                   g/*
                   (range n))))

(defmethod rising-factorial [::v/native-integral ::v/native-integral] [x n]
  (cond (zero? n) 1
        (neg? n)
        (let [denom (falling-factorial (dec x) (- n))]
          (if (zero? denom)
            ##Inf
            (/ 1 denom)))

        :else
        (let [elems (range x (+ x n))]
          #?(:clj
             (apply *' elems)
             :cljs
             (transduce (map u/bigint) * elems)))))

;; https://www.johndcook.com/blog/2010/09/21/variations-on-factorial/

;; https://www.johndcook.com/blog/2021/10/14/multifactorial/

;; https://en.wikipedia.org/wiki/Double_factorial#Generalizations

(defn multi-factorial
  [n k]
  ;; TODO double checkw with wolfram alpha, what are the conditions?
  ;;
  ;; TODO thanks for the implementation here!
  ;; https://www.johndcook.com/blog/2021/10/14/multifactorial/
  {:pre [(v/native-integral? n)
         (v/native-integral? k)
         (>= n 0), (> k 0)]}
  (let [elems (range n 0 (- k))]
    #?(:clj
       (reduce *' elems)
       :cljs
       (transduce (map u/bigint) g/* elems))))

;; double factorial notes:
;; https://www.johndcook.com/blog/2010/09/21/variations-on-factorial/

(defn double-factorial
  "Why did we define this separately? Note that this worls for negative arguments!
  https://en.wikipedia.org/wiki/Double_factorial#Negative_arguments"
  [n]
  {:pre [(v/native-integral? n)]}
  (cond (zero? n) 1
        (pos? n)  (multi-factorial n 2)
        (even? n) ##Inf
        :else (g/div
               (double-factorial (+ n 2))
               (+ n 2))))

(defn subfactorial
  "https://mathworld.wolfram.com/Subfactorial.html

   https://www.johndcook.com/blog/2010/09/21/variations-on-factorial/

  get the number of derangements of size n
  https://en.wikipedia.org/wiki/Derangement

  More details: https://www.johndcook.com/blog/2010/04/06/subfactorial/"
  [n]
  (if (zero? n)
    1
    (let [nf-div-e (g/div (factorial n) Math/E)]
      (g/floor
       (g/add 0.5 nf-div-e)))))

;; TODO close that ticket after we get this in.

(defn binomial-coefficient
  [n m]
  {:pre [(<= 0 n m)]}
  ;; TODO move the good code over to here, and call it from
  ;; number-of-combinations!
  #_(number-of-combinations n m))


;; TODO handle the cljs case where we need bigint!
;;
;; TODO tell GJS, signed vs unsigned, get a -n in there:
;; https://en.wikipedia.org/wiki/Stirling_numbers_of_the_first_kind

;; TODO tell GJS assert is wrong, k can be zero:
;;
;; TODO figure out asserts, we can totally have a 0 k, and have to figure out
;; the rules for when we return a 0.

(defn stirling-first-kind
  "TODO calculated using recurrence relation here:
  https://en.wikipedia.org/wiki/Stirling_numbers_of_the_first_kind

  This is the SIGNED.

  TODO take a keyword arg for signed"
  [n k]
  {:pre [(<= 0 k)
         (<= k n)]}
  (let [rec  (atom nil)
        rec* (fn [n k]
               (if (zero? n)
                 (if (zero? k) 1 0)
                 (let [n-1 (dec n)]
                   (+' (@rec n-1 (dec k))
                       (*' (- n-1) (@rec n-1 k))))))]
    (reset! rec (memoize rec*))
    (@rec n k)))

;; stack overflows, weird guards here?
;;
;; recurrence works if k>0... do StirlingS2 to figure out what is up:
;; https://www.wolframalpha.com/input/?i=StirlingS1%5B1%2C4%5D
(defn stirling-second-kind [n k]
  {:pre [(<= 1 k)
         (<= k n)]}
  (let [rec  (atom nil)
        rec* (fn [n k]
               (cond (= k 1) 1
	                   (= n k) 1
	                   :else
	                   (let [n-1 (dec n)]
		                   (+' (*' k (@rec n-1 k))
		                       (@rec n-1 (dec k))))))]
    (reset! rec (memoize rec*))
    (@rec n k)))
