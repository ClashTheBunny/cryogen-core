(ns cryogen-core.toc
  (:require [crouton.html :as html]
            [hiccup.core :as hiccup]))

(def _h [:h1 :h2 :h3 :h4 :h5 :h6])
(defn- compare_index [i1 i2] (- (.indexOf _h i2) (.indexOf _h i1)))

(defn- get-headings
  "Turn a body of html content into a vector of elements whose tags are
  headings."
  [content]
  (reduce
    (fn [headings {:keys [tag attrs content] :as elm}]
      (if (some #{tag} _h)
        (conj headings elm)
        (if-let [more-headings (get-headings content)]
          (into headings more-headings)
          headings)))
    [] content))

(defn make-links
  "Create a table of contents from the given headings. This function will look
  for either:
  (1) headings with a child anchor with a non-nil name attribute, e.g.
      <h1><a name=\"reference\">Reference Title</a></h1>
  or
  (2) headings with an id attribute, e.g. <h1 id=\"reference\">Reference Title</h1>
  In both cases above, the anchor reference becomes \"#reference\" and the
  anchor text is \"Reference Title\"."
  [headings]
  (loop [items headings acc nil _last nil]
    (if-let [{tag :tag {id :id} :attrs [{{name :name} :attrs} title :as htext] :content} (first items)]
      (let [anchor (or id name)]
        (if (nil? anchor)
          (recur (rest items) acc nil)
          (let [entry [:li [:a {:href (str "#" anchor)} (or title (first htext))]]
                jump (compare_index _last tag)]
            (cond (> jump 0) (recur (rest items) (str acc "<ol>" (hiccup/html entry)) tag)
                  (= jump 0) (recur (rest items) (str acc (hiccup/html entry)) tag)
                  (< jump 0) (recur (rest items) (str acc (apply str (repeat (* -1 jump) "</ol>"))
                                                      (hiccup/html entry)) tag)))))
      (str acc "</ol>"))))

(defn generate-toc [html]
  (-> html
      (.getBytes "UTF-8")
      (java.io.ByteArrayInputStream.)
      (html/parse)
      :content
      (get-headings)
      (make-links)
      (clojure.string/replace-first #"ol" "ol class=\"contents\"")))
