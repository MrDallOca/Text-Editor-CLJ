(ns editor-clj.key-resolver
  (:require [clojure.string :as str])
  (:import [jline.console ConsoleReader]))

(defn get-chars-map
  "Use it and see the magic"
  ([& s-e]
   (let [s-e (partition 2 s-e)]
     (->> (for [[s e] s-e]
            (range (int s) (+ (int e) 1))) 
          flatten
          (reduce (fn [acc v]
                    (assoc acc (-> v char str) v))
                  {})))))

(def ctrl-keys
  (->> (range (int \) (+ (int \) 1)) 
       (reduce (fn [acc v]
                 (assoc acc (->> v (+ 96) char (str "CTRL-")) v))
               {})))


(def avaiable-keys
  (-> (get-chars-map \a \z
                     \A \Z
                     \0 \9)
      (into ctrl-keys)))

(defn create-binding
  "Creates a vector representing a keybinding which can be resolved later.
  It takes a string binding and a impure f(unction)."
  [binding f]
  [(->> (str/split binding #" ")
        (map #(get avaiable-keys %))),
   f])


(defn append-binding
  "Appends a binding to a hash-map of bindings"
  [hash binding f]
  (let [new-binding (create-binding binding f)
        [k v] new-binding]
    (assoc hash k v)))

(defn find-candidates
  "Finds candidates"
  [map value]
  (->> (for [[k v] map
             :when (= (first k) value)]
         [(drop 1 k) v]) 
       (into {})))

(defn resolve-keystroke
  "Resolves a keystroke based on a hash-map of bindings if finded.
  If not, default-fn will be evaluated" 
  [hash reader default-fn]
  (flush) 
  (let [the-one (loop [code (.readCharacter reader)
                       candidates (find-candidates hash code)]
                  (if (> (count candidates)
                         1) 
                    (let [code (.readCharacter reader)
                          candidates (find-candidates candidates code)]
                      (recur code candidates))
                    candidates))]
    (if (not (empty? the-one))
      ((first (vals the-one)))
      (default-fn))))

(defn find-keystroke
  "Finds (but doesn't execute) a binding based on a hash-map of bindings
  and a sequence of keystrokes (seq of key codes)"
  [hash keys] 
  (let [ret (loop [code (first keys)
                   keys (drop 1 keys)
                   candidates (find-candidates hash code)]
              (if (or (> (count candidates) 1)
                      (> (count keys) 1)) 
                (let [code (first keys)
                      keys (drop 1 keys)
                      candidates (find-candidates candidates code)]
                  (recur code keys candidates))
                candidates))]
    (when (not (empty? ret))
      (first (vals ret)))))

