(ns pl.mrroman.clj-start.test-runner
  (:require
   [clojure.test :as t]
   [pl.mrroman.clj-start.class-cache :refer [with-class-cache]]
   [pl.mrroman.clj-start.sources :as sources])
  (:gen-class))

(with-class-cache
  (require '[lambdaisland.deep-diff2 :as ddiff]))

(def no-colors-printer (ddiff/printer {:print-color false}))

(defmulti vim-report :type)

(defmethod vim-report :begin-test-ns [m] (println "\nTesting" (ns-name (:ns m))))
(defmethod vim-report :begin-test-var [m]
  (println "\nExecuting" (:name (meta (:var m)))))

(defmethod vim-report :fail
  [m]
  (t/inc-report-counter :fail)
  (when-let [source-file (some-> t/*testing-vars*
                                 first
                                 meta
                                 :file)]
    (println (str "FAIL:" source-file ":" (:line m) ":" (t/testing-vars-str m) ":" (t/testing-contexts-str) ":" (:message m "FAIL")))
    (println (str "FAIL-CONTINUE:EXPECTED:" (pr-str (:expected m))))
    (println (str "FAIL-CONTINUE:ACTUAL:" (pr-str (:actual m))))))

(defmethod t/assert-expr '= [msg form]
  (let [args (rest form)
        pred (first form)]
    `(let [values# (list ~@args)
           result# (apply ~pred values#)]
       (if result#
         (t/do-report {:type :pass, :message ~msg,
                       :expected '~form, :actual (cons '~pred values#)})
         (do
           (t/do-report {:type :fail, :message ~msg,
                         :expected '~form, :actual (list '~'not (cons '~pred values#))})
           (-> (apply ddiff/diff values#)
               (ddiff/pretty-print no-colors-printer))))
       result#)))

(defn- find-line-number [source-file m]
  (if (instance? Throwable (:actual m))
    (let [fname (-> source-file (java.io.File.) (.getName))]
      (->> m
           :actual
           Throwable->map
           :trace
           (some (fn [[_ _ e-file e-line]]
                   (when (= e-file fname)
                     e-line)))))
    (:line m)))

(defmethod vim-report :error
  [m]
  (t/inc-report-counter :error)
  (when-let [source-file (some-> t/*testing-vars*
                                 first
                                 meta
                                 :file)]
    (let [line (find-line-number source-file m)]
      (println (str "ERROR:" source-file ":" line ":" (t/testing-vars-str m) ":" (t/testing-contexts-str) ":" (:message m "FAIL")))
      (println (str "ERROR-CONTINUE:EXPECTED:" (pr-str (:expected m))))
      (println (str "ERROR-CONTINUE:ACTUAL:"
                    (if (instance? Throwable (:actual m))
                      (ex-message (:actual m))
                      (pr-str (:actual m)))))
      (when (instance? Throwable (:actual m))
        (.printStackTrace (:actual m))))))

(defmethod vim-report :default
  [_])

(defn find-closest-test [test-file test-line]
  (->> (all-ns)
       (mapcat ns-publics)
       (map second)
       (filter (comp :test meta))
       (filter (comp #{test-file} :file meta))
       (map #(vector % (- test-line (-> % meta :line))))
       (filterv (comp pos? second))
       (sort-by second)
       first
       first))

(defn- instrument-malli! []
  (when (find-ns 'malli.core)
    (println "Malli detected. Instrument functions...")
    (require 'malli.dev)
    (require 'malli.dev.pretty)
    ((find-var 'malli.dev/start!) {:report ((find-var 'malli.dev.pretty/thrower))})))

(defn- execute-tests [test-file test-line test-files test-namespaces]
  (if (and test-file test-line)
    (if-let [test-var (find-closest-test (first test-files) (parse-long test-line))]
      (->> (t/run-test-var test-var)
           ((juxt :fail :error))
           (apply +))
      (do
        (println "No test found")
        0))
    (reduce (fn [total-fails n]
              (let [results (t/run-tests n)]
                (+ total-fails
                   (:fail results 0)
                   (:error results 0))))
            0
            test-namespaces)))

(defn -main [& {:strs [-test-file -test-line] :or {-test-file "test"}}]
  (with-class-cache
    (compile 'pl.mrroman.clj-start.test-runner)
    (println "Detecting test files in" -test-file)
    (let [test-files (sources/find-paths -test-file)]
      (println "Loading test files...")
      (run! load-file test-files)
      (instrument-malli!)
      (let [test-namespaces (sources/find-namespaces test-files true)]
        (with-redefs [t/report vim-report]
          (System/exit
           (if (pos? (execute-tests -test-file -test-line test-files test-namespaces))
             1
             0)))))))
