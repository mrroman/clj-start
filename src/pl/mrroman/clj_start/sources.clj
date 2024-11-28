(ns pl.mrroman.clj-start.sources
  (:import
   [java.io File]))

(defn clj-file? [^File f]
  (re-matches #"^.*\.clj[sc]?$" (.getName f)))

(defn find-paths [^String file-or-dir]
  (->> file-or-dir
       (java.io.File.)
       (file-seq)
       (filter (memfn isFile))
       (filter clj-file?)
       (map (memfn getAbsolutePath))
       (set)))

(defn find-namespaces [files with-tests?]
  (into #{}
        (comp
         (mapcat ns-publics)
         (map (comp meta second))
         (filter (if with-tests? :test any?))
         (filter (comp files :file))
         (map :ns))
        (all-ns)))
