(ns editor-clj.buffer
  (:require [editor-clj.utils :refer :all]
            [editor-clj.doubly-linked-list :refer :all])
  (:import [jline.console ConsoleReader]
           [java.util Stack]))

(def editor-state
  (atom {:lines nil
         :curr-coln nil
         :curr-row nil
         :insert-mode nil}))

(def state-stack (atom '()))

(add-watch editor-state :add-stack
           (fn [k r o n]
             (swap! state-stack conj o)))

(defn can-undo?
  []
  (not ;;(or (empty? action-stack)
   (empty? @state-stack)))

(defn undo!
  "Pop the action and the state stack"
  []
  (when (can-undo?)
    (reset! editor-state (peek @state-stack))
    (swap! state-stack #(-> (pop %) pop))))

(defn get-editor
  [& k]
  (get-in @editor-state k))

(defn update-editor!
  "Updates the editor state, where f is a function that will do the magic
  and k is a keyword that indicates what will be updated"
  ([k f]
   (swap! editor-state update k f))
  ([k f & args]
   (swap! editor-state update k f args)))

(defn update-in-editor!
  "Updates the editor state, where f is a function that will do the magic
  and ks is a keyword vector that indicates what will be updated"
  ([ks f]
   (swap! editor-state update-in ks f))
  ([ks f & args]
   (swap! editor-state update-in ks f args)))

(defn set-editor!
  "No docsting provided"
  [k v]
  (swap! editor-state assoc k v))

(defn start-writing!
  "Configurates the global state to write"
  []
  (set-editor! :lines (dbl '("")))
  (set-editor! :curr-coln 0)
  (set-editor! :curr-row 0)
  (set-editor! :insert-mode true))

(defn toggle-insert
  "Toggles insert-mode"
  []
  (update-editor! :insert-mode not))

(defn breakline
  "Breaks the curr(ent) line in the curr(ent) coln"
  []
  (let [coln (get-editor :curr-coln)]
    (update-editor! :lines #(breakline-at % coln))
    (set-editor! :curr-coln 0)
    (update-editor! :curr-row inc)))

(defn insert-char
  "Inserts a char at the curr(ent) position"
  [c]
  (let [ln (get-editor :lines)
        coln (get-editor :curr-coln)
        curr (curr ln)
        curr-s (.length curr)]
    (do (update-in-editor! [:lines :curr]
                           #((if (or (get-editor :insert-mode)
                                     (== coln curr-s))
                               str-insert
                               str-replace)
                             % c coln))
        (update-editor! :curr-coln inc))))

(defn backspace
  "Does exactly what you'd expect backspace to do"
  []
  (let [coln (get-editor :curr-coln)]
    (if (> coln 0)
      (do (update-in-editor! [:lines :curr] #(str-delete % (dec coln)))
          (update-editor! :curr-coln dec)) 
      (when-let [pln (peek (get-editor :lines :prevs))]
        (update-editor! :lines #(-> (backward %)
                                    (merge-lines)))
        (set-editor! :curr-coln (.length pln))
        (update-editor! :curr-row dec)))))

(defn delete
  "Deletes the char at the curr(ent) position"
  []
  (let [coln (get-editor :curr-coln)
        ln (get-editor :lines)
        curr (:curr ln)] 
    (if (and (> coln 0) (> (.length curr) 0))
      (update-in-editor! [:lines :curr] #(str-delete % coln))
      (when (pick-next ln)
        (update-editor! :lines #(merge-lines %))))))

(defn upward
  "Moves upward"
  [] 
  (let [pln (pick-prev (get-editor :lines))
        coln (get-editor :curr-coln)]
    (when pln
      (do (update-editor! :lines #(backward %))
          (update-editor! :curr-row dec)
          (set-editor! :curr-coln
                       (let [pln-s (.length pln)]
                         (if (< coln pln-s)
                           coln pln-s)))))))

(defn downward
  "Moves upward"
  [] 
  (let [nln (pick-next (get-editor :lines))
        coln (get-editor :curr-coln)]
    (when nln
      (do (update-editor! :lines #(forward %))
          (update-editor! :curr-row inc)
          (set-editor! :curr-coln
                       (let [nln-s (.length nln)]
                         (if (< coln nln-s)
                           coln nln-s)))))))

(defn righthward
  "Moves righthward"
  []
  (let [coln   (get-editor :curr-coln)
        ln     (get-editor :lines)
        curr-s (.length (:curr ln))]
    (if (< coln curr-s)
      (update-editor! :curr-coln inc)
      (when-let [nln (pick-next ln)]
        (do (update-editor! :lines #(forward %))
            (update-editor! :curr-row inc)
            (set-editor! :curr-coln 0))))))

(defn leftward
  "Moves leftward"
  []
  (let [coln (get-editor :curr-coln)
        ln   (get-editor :lines)]
    (if (> coln 0)
      (update-editor! :curr-coln dec)
      (when-let [pln (pick-next ln)]
        (do (update-editor! :lines #(backward %))
            (update-editor! :curr-row dec)
            (set-editor! :curr-coln
                         (-> (.length pln)
                             (dec))))))))

