
(ns syntax-interpreter.interpreter.engine
    (:require [syntax-interpreter.interpreter.metafunctions :as interpreter.metafunctions]
              [syntax-interpreter.interpreter.utils         :as interpreter.utils]))

;; ----------------------------------------------------------------------------
;; ----------------------------------------------------------------------------

(defn interpreter
  ; @description
  ; - Applies the given 'f' function at each cursor position of the given 'n' string.
  ; - Provides a state of the actual position and a set of metafunctions for the applied function.
  ; - The provided state contains the 'actual-tags' vector that describes the opened tags at the actual position
  ;   and contains the 'left-tags' map that contains tags that are already ended before the actual position and
  ;   removed from the 'actual-tags' vector.
  ;
  ; @param (string) n
  ; @param (function) f
  ; Applied at each cursor position.
  ; Takes the output of the previous iteration (or the given 'initial' value) as first parameter.
  ; Takes the state of the actual cursor position as second parameter.
  ; Takes a map with metafunctions as third parameter.
  ; @param (*) initial
  ; @param (vectors in vector)(opt)(in decreasing priority order) tags
  ; [[(keyword) tag-name
  ;   (regex pattern) pattern / opening-pattern
  ;   (regex pattern)(opt) closing-pattern
  ;   (map)(opt) options
  ;   {:accepted-ancestors (keywords in vector or empty vector)(opt)
  ;     Processes the tag only if at least one of the accepted ancestor tags is opened.
  ;     Leave this vector empty for tags that are processed only if they have no ancestor tags.
  ;    :accepted-children (keywords in vector or empty vector)(opt)
  ;     Processes child tags only if listed in this vector.
  ;     Leave this vector empty for void tags that cannot have children.
  ;    :accepted-descendants (keywords in vector or empty vector)(opt)
  ;     Processes descendant tags only if listed in this vector.
  ;     Leave this vector empty for void tags that cannot have descendants.
  ;    :accepted-parents (keywords in vector or empty vector)(opt)
  ;     Processes the tag only if at least one of the accepted parent tags is opened.
  ;     Leave this vector empty for tags that are processed only if they have no parent tags.
  ;    :pattern-limits (map)(opt)
  ;     Limited pattern lookaround and match lengths help decrease the processing time
  ;     and help create more accurate matches for tag patterns with lookaround assertions.
  ;     {:lookahead (integer)(opt)
  ;       Default: 0
  ;      :lookbehind (integer)(opt)
  ;       Default: 0
  ;      :match (integer)(opt)
  ;       Default: 64
  ;      :closing/lookahead (integer)(opt)
  ;       Default: 0
  ;      :closing/lookbehind (integer)(opt)
  ;       Default: 0
  ;      :closing/match (integer)(opt)
  ;       Default: 64
  ;      :opening/lookahead (integer)(opt)
  ;       Default: 0
  ;      :opening/lookbehind (integer)(opt)
  ;       Default: 0
  ;      :opening/match (integer)(opt)
  ;       Default: 64}]]
  ; @param (map)(opt) options
  ; {:endpoint (integer)(opt)
  ;   Stops the interpreter at the given 'endpoint' position.
  ;  :offset (integer)(opt)
  ;   Starts applying the given 'f' function at the given 'offset' position.
  ;   In order to make accurate tag map, the interpreter starts processing at the 0th position even if the 'offset' value is higher than 0.}
  ;
  ; @usage
  ; (interpreter "My string" (fn [result state metafunctions]) nil)
  ;
  ; @usage
  ; (interpreter "My string" println nil)
  ;
  ; @usage
  ; (let [my-text     "abc(def(ghi))"
  ;       my-function (fn [result state _] (if (= 8 (:cursor state)) state result)
  ;       my-initial  nil
  ;       my-tags     [[:paren #"\(" #"\)"]]
  ;     (interpreter my-text my-function my-initial my-tags)
  ; =>
  ; {:actual-tags [{:name :paren :started-at 3 :opened-at 4}
  ;                {:name :paren :started-at 7 :opened-at 8}]
  ;  :cursor 8}
  ;
  ; @usage
  ; (let [my-text     "<div>Hello World!</div>"
  ;       my-function (fn [result state _] (if (= 8 (:cursor state)) state result)
  ;       my-initial  nil
  ;       my-tags     [[:div #"\<div\>" #"\<\/div\>"]]
  ;     (interpreter my-text my-function my-initial my-tags)
  ; =>
  ; {:actual-tags [{:name :div :started-at 0 :opened-at 5}]
  ;  :cursor 8}
  ;
  ; @return (*)
  ([n f initial]
   (interpreter n f initial {} {}))

  ([n f initial tags]
   (interpreter n f initial tags {}))

  ([n f initial tags options]
   (letfn [; @description
           ; Returns the 'metafunctions' map that is provided for the applied 'f' function at each cursor position.
           ;
           ; @param (map) state
           ;
           ; @return (map)
           ; {:ancestor-tags (function)
           ;  :closing-tag (function)
           ;  :depth (function)
           ;  :ending-tag (function)
           ;  :left-sibling-count (function)
           ;  :no-tags-opened? (function)
           ;  :opening-tag (function)
           ;  :parent-tag (function)
           ;  :reading-any-closing-match? (function)
           ;  :reading-any-opening-match? (function)
           ;  :reading-any-match? (function)
           ;  :starting-tag (function)
           ;  :stop (function)
           ;  :tag-ancestor? (function)
           ;  :tag-body (function)
           ;  :tag-closed-at (function)
           ;  :tag-closed? (function)
           ;  :tag-closes? (function)
           ;  :tag-content (function)
           ;  :tag-depth (function)
           ;  :tag-ends? (function)
           ;  :tag-left-count (function)
           ;  :tag-met-count (function)
           ;  :tag-opened-at (function)
           ;  :tag-opened? (function)
           ;  :tag-opens? (function)
           ;  :tag-parent? (function)
           ;  :tag-started-at (function)
           ;  :tag-started? (function)
           ;  :tag-starts? (function)
           ;  :use-metadata (function)}
           (f0 [state]
               {:ancestor-tags              (interpreter.metafunctions/ancestor-tags-f             n tags options state)
                :closing-tag                (interpreter.metafunctions/closing-tag-f               n tags options state)
                :depth                      (interpreter.metafunctions/depth-f                     n tags options state)
                :ending-tag                 (interpreter.metafunctions/ending-tag-f                n tags options state)
                :left-sibling-count         (interpreter.metafunctions/left-sibling-count-f        n tags options state)
                :no-tags-opened?            (interpreter.metafunctions/no-tags-opened-f            n tags options state)
                :opening-tag                (interpreter.metafunctions/opening-tag-f               n tags options state)
                :parent-tag                 (interpreter.metafunctions/parent-tag-f                n tags options state)
                :reading-any-closing-match? (interpreter.metafunctions/reading-any-closing-match-f n tags options state)
                :reading-any-opening-match? (interpreter.metafunctions/reading-any-opening-match-f n tags options state)
                :reading-any-match?         (interpreter.metafunctions/reading-any-match-f         n tags options state)
                :starting-tag               (interpreter.metafunctions/starting-tag-f              n tags options state)
                :stop                       (interpreter.metafunctions/stop-f                      n tags options state)
                :tag-ancestor?              (interpreter.metafunctions/tag-ancestor-f              n tags options state)
                :tag-body                   (interpreter.metafunctions/tag-body-f                  n tags options state)
                :tag-closed-at              (interpreter.metafunctions/tag-closed-at-f             n tags options state)
                :tag-closed?                (interpreter.metafunctions/tag-closed-f                n tags options state)
                :tag-closes?                (interpreter.metafunctions/tag-closes-f                n tags options state)
                :tag-closing-pattern        (interpreter.metafunctions/tag-closing-pattern-f       n tags options state)
                :tag-content                (interpreter.metafunctions/tag-content-f               n tags options state)
                :tag-depth                  (interpreter.metafunctions/tag-depth-f                 n tags options state)
                :tag-details                (interpreter.metafunctions/tag-details-f               n tags options state)
                :tag-ends?                  (interpreter.metafunctions/tag-ends-f                  n tags options state)
                :tag-left-count             (interpreter.metafunctions/tag-left-count-f            n tags options state)
                :tag-met-count              (interpreter.metafunctions/tag-met-count-f             n tags options state)
                :tag-opened-at              (interpreter.metafunctions/tag-opened-at-f             n tags options state)
                :tag-opened?                (interpreter.metafunctions/tag-opened-f                n tags options state)
                :tag-opening-pattern        (interpreter.metafunctions/tag-opening-pattern-f       n tags options state)
                :tag-opens?                 (interpreter.metafunctions/tag-opens-f                 n tags options state)
                :tag-options                (interpreter.metafunctions/tag-options-f               n tags options state)
                :tag-parent?                (interpreter.metafunctions/tag-parent-f                n tags options state)
                :tag-started-at             (interpreter.metafunctions/tag-started-at-f            n tags options state)
                :tag-started?               (interpreter.metafunctions/tag-started-f               n tags options state)
                :tag-starts?                (interpreter.metafunctions/tag-starts-f                n tags options state)
                :use-metadata               (interpreter.metafunctions/use-metadata-f              n tags options state)})

           ; @description
           ; Returns the function that is applied by the interpreter in the actual iteration.
           ; If the actual cursor didn't reach the given 'offset' value, it returns a noop function.
           ;
           ; @param (map) state
           ;
           ; @return (map)
           (f1 [state]
               (if (interpreter.utils/offset-reached? n tags options state)
                   (fn [result state metafunctions] (try (f result state metafunctions)
                                                         (catch Exception e (println e))))
                   (fn [result _ _] (-> result))))]

          ; ...
          (let [initial-state {:actual-tags [] :left-tags {} :cursor 0 :result initial}]
               (loop [{:keys [result] :as state} initial-state]
                     (let [actual-state           (interpreter.utils/update-previous-state n tags options state)
                           provided-state         (interpreter.utils/filter-provided-state n tags options actual-state)
                           provided-metafunctions (-> actual-state f0)
                           applied-function       (-> actual-state f1)
                           updated-result         (-> result (applied-function provided-state provided-metafunctions))
                           updated-state          (interpreter.utils/update-actual-state n tags options actual-state updated-result)]
                          (cond (interpreter.utils/iteration-stopped? n tags options updated-state) (-> updated-state :result)
                                (interpreter.utils/endpoint-reached?  n tags options updated-state) (-> updated-state :result)
                                (interpreter.utils/iteration-ended?   n tags options updated-state) (-> updated-state :result)
                                :next-iteration (let [prepared-state (interpreter.utils/prepare-next-state n tags options updated-state)]
                                                     (recur prepared-state)))))))))
