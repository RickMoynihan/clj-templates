(ns clj-templates.core
  (:require [clj-templates.util :as util]
            [clj-templates.lex :as lex]
            [clj-templates.parse :as parse]
            [clj-templates.expansion :as ex]
            [clojure.walk :as walk])
  (:refer-clojure :exclude [compile])
  (:import [clojure.lang Named]
           [java.lang Number]))

(defprotocol IntoValue
  (to-value [this]))

(def into-string {:to-value str})
(def into-identity {:to-value identity})
(extend Named IntoValue {:to-value name})
(extend String IntoValue into-identity)
(extend nil IntoValue into-identity)
(extend Number IntoValue into-string)
(extend Boolean IntoValue into-string)
(extend Character IntoValue into-string)

(defn- format-value [v]
  (cond
    (map? v) (into {} (map (fn [[k v]] [(name k) (to-value v)]) v))
    (sequential? v) (into (empty v) (map to-value v))
    :else (to-value v)))

(defn format-bindings [bindings]
  (into {} (map (fn [[k v]]
                  [(name k) (format-value v)])
                bindings)))

(defn- template-valid? [template]
  (every? (fn [section]
            (contains? #{:expression :literals} (:type section)))
          template))

(defn compile [template]
  (let [lexbuf (lex/from-string template)
        t (parse/parse parse/uri-template lexbuf)]
    (fn [bindings]
      (let [bindings (format-bindings bindings)
            cps (ex/expand-template t bindings)
            result (util/codepoints->string cps)]
        (if (template-valid? t)
          result
          (throw (ex-info "Invalid URI template" {:expansion result})))))))



(defn expand
  "Expands a URI template string with the a map of bindings"
  [template bindings]
  (let [bindings (format-bindings bindings)
        lexbuf (lex/from-string template)
        t (parse/parse parse/uri-template lexbuf)
        cps (ex/expand-template t bindings)
        result (util/codepoints->string cps)]
    (if (template-valid? t)
      result
      (throw (ex-info "Invalid URI template" {:expansion result})))))
