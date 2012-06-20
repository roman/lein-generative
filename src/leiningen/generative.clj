(ns leiningen.generative
  (:refer-clojure :exclude [test])
  (:require leiningen.core)
  (:use [leiningen.compile :only [eval-in-project]]
        [leiningen.classpath :only [classpath]]))

(defn- run-generative-tests [project]
  `(do
     (let [path# ~(or (:generative-path project) (:test-path project))]
       (println "Testing generative tests in" path#
                "on" gen/*cores* "cores for"
                gen/*msec* "msec.")
       (let [futures# (gen/test-dirs path#)]
         (doseq [f# futures#]
           (try
             @f#
             (catch Throwable t#
               (.printStackTrace t#)
               (System/exit -1))
             (finally
              (when-not ~leiningen.core/*interactive?*
                (shutdown-agents)))))))))

(defn- add-generative-path-to-classpath [project]
  (update-in project
             [:extra-classpath-dirs]
             #(conj % (:generative-path project))))

(defn add-generative-dependency [project]
  (if (some #(= 'org.clojure/test.generative (first %)) (:dependencies project))
    project
    (update-in project [:dependencies]
               conj '[org.clojure/test.generative "0.1.4"])))

(defn generative
  "Run test.generative tests"
  [project & _]
  (let [new-project (-> project
                        add-generative-dependency
                        add-generative-path-to-classpath)]
    (eval-in-project
      new-project
      (run-generative-tests new-project)
      nil
      nil
      '(require '[clojure.test.generative :as gen]))))
