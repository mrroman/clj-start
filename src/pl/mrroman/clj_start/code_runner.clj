(ns pl.mrroman.clj-start.code-runner
  (:require
   [clojure.java.io :as io]
   [pl.mrroman.clj-start.class-cache :refer [with-class-cache]]
   [pl.mrroman.clj-start.sources :as sources])
  (:gen-class))

(defn -main [file & args]
  (with-class-cache
    (compile 'pl.mrroman.clj-start.code-runner)
    (let [main-file (.getAbsolutePath (io/file file))]
      (load-file main-file)
      (let [loaded-ns (first (sources/find-namespaces #{main-file} false))]
        (try
          (some-> loaded-ns
                  ns-name
                  name
                  (symbol "-main")
                  find-var
                  (apply args))
          (catch Throwable t
            (.printStackTrace t)
            (System/exit 1)))))))
