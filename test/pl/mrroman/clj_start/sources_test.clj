(ns pl.mrroman.clj-start.sources-test
  (:require
   [clojure.test :refer [are deftest is]]
   [pl.mrroman.clj-start.sources :as sut])
  (:import
   [java.io File]
   [java.nio.file Files]
   [java.nio.file.attribute FileAttribute]))

(deftest clj-file?-test
  (are [n res] (= res (boolean (sut/clj-file? (java.io.File. n))))
    "hello.clj" true
    "hello.cljs" true
    "hello.cljc" true
    "hello.cl" false
    "" false))

(deftest find-paths-test
  (let [root (.toFile (Files/createTempDirectory "find-paths" (into-array FileAttribute [])))]
    (.mkdir root)
    (.createNewFile (File. root "hello.clj"))
    (.createNewFile (File. root "hello.txt"))
    (.mkdir (File. root "dir1"))
    (.createNewFile (File. (File. root "dir1") "foo.clj"))
    (is (= #{(str root "/dir1/foo.clj")
             (str root "/hello.clj")}
           (set (sut/find-paths (.getAbsolutePath root)))))))

(deftest find-namespaces-test
  (let [test-files #{(.getAbsolutePath (File. "src/pl/mrroman/clj_start/sources.clj"))
                     (.getAbsolutePath (File. "test/pl/mrroman/clj_start/sources_test.clj"))}]
    (run! load-file test-files)
    (is (= #{(find-ns 'pl.mrroman.clj-start.sources-test)}
           (sut/find-namespaces test-files true)))
    (is (= #{(find-ns 'pl.mrroman.clj-start.sources)
             (find-ns 'pl.mrroman.clj-start.sources-test)}
           (sut/find-namespaces test-files false)))))
