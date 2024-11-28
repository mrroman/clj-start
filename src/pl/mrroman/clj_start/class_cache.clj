(ns pl.mrroman.clj-start.class-cache)

(defmacro with-class-cache [& body]
  `(binding [*compile-path*  ".cache/classes"
             *compile-files* true]
     (.mkdirs (java.io.File. *compile-path*))
     ~@body))

