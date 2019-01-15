package com.ustadmobile.lib.contentscrapers;

import java.util.Arrays;
import java.util.List;

public class ScraperConstants {

    public static final String CONTENT_JSON = "content.json";
    public static final String QUESTIONS_JSON = "questions.json";
    public static final String ETAG_TXT = "etag.txt";
    public static final String LAST_MODIFIED_TXT = "last-modified.txt";
    public static final String ABOUT_HTML = "about.txt";
    public static final String VIDEO_MP4 = "video.mp4";
    public static final String UTF_ENCODING = "UTF-8";
    public static final String EDRAAK_INDEX_HTML_TAG = "/com/ustadmobile/lib/contentscrapers/edraak/index.html";
    public static final String CK12_INDEX_HTML_TAG = "/com/ustadmobile/lib/contentscrapers/ck12/index.html";
    public static final String JS_TAG = "/com/ustadmobile/lib/contentscrapers/jquery-3.3.1.min.js";
    public static final String MATERIAL_JS_LINK = "/com/ustadmobile/lib/contentscrapers/materialize.min.js";
    public static final String MATERIAL_CSS_LINK = "/com/ustadmobile/lib/contentscrapers/materialize.min.css";
    public static final String REGULAR_ARABIC_FONT_LINK = "/com/ustadmobile/lib/contentscrapers/edraak/DroidNaskh-Regular.woff2";
    public static final String BOLD_ARABIC_FONT_LINK = "/com/ustadmobile/lib/contentscrapers/edraak/DroidNaskh-Bold.woff2";
    public static final String CIRCULAR_CSS_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/css-circular-prog-bar.css";
    public static final String LANGUAGE_LIST_LOCATION = "/com/ustadmobile/lib/contentscrapers/iso_639_3.json";
    public static final String CIRCULAR_CSS_NAME = "css-circular-prog-bar.css";
    public static final String TIMER_PATH = "/com/ustadmobile/lib/contentscrapers/ck12/timer.svg";
    public static final String TIMER_NAME = "timer.svg";
    public static final String TROPHY_PATH = "/com/ustadmobile/lib/contentscrapers/ck12/trophy.svg";
    public static final String TROPHY_NAME = "trophy.svg";
    public static final String CHECK_PATH = "/com/ustadmobile/lib/contentscrapers/ck12/check.svg";
    public static final String CHECK_NAME = "check.svg";
    public static final String XML_NAMESPACE = "http://purl.org/dc/elements/1.1/";

    public static final String CORRECT_KHAN_LINK = "/com/ustadmobile/lib/contentscrapers/khan/exercise-correct.svg";
    public static final String CORRECT_FILE = "exercise-correct.svg";

    public static final String ATTEMPT_KHAN_LINK = "/com/ustadmobile/lib/contentscrapers/khan/star-attempt.svg";
    public static final String ATTEMPT_FILE = "star-attempt.svg";

    public static final String COMPLETE_KHAN_LINK = "/com/ustadmobile/lib/contentscrapers/khan/star-complete.svg";
    public static final String COMPLETE_FILE = "star-complete.svg";

    public static final String TRY_AGAIN_KHAN_LINK = "/com/ustadmobile/lib/contentscrapers/khan/exercise-try-again.svg";
    public static final String TRY_AGAIN_FILE = "exercise-try-again.svg";

    public static final String QUIZ_HTML_LINK = "/com/ustadmobile/lib/contentscrapers/voa/quiz.html";
    public static final String QUIZ_HTML_FILE = "quiz.html";

    public static final String IFRAME_RESIZE_LINK = "/com/ustadmobile/lib/contentscrapers/voa/iframeResizer.min.js";
    public static final String IFRAME_RESIZE_FILE = "iframeResizer.min.js";

    public static final String IFRAME_RESIZE_WINDOW_LINK = "/com/ustadmobile/lib/contentscrapers/voa/iframeResizer.contentWindow.min.js";
    public static final String IFRAME_RESIZE_WINDOW_FILE = "iframeResizer.contentWindow.min.js";

    // math jax links
    public static final String MATH_JAX_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/MathJax.js";
    public static final String MATH_JAX_FILE = "MathJax.js";

    public static final String JAX_CONFIG_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/TeX-AMS-MML_HTMLorMML.js";
    public static final String JAX_CONFIG_FILE = "/config/TeX-AMS-MML_HTMLorMML.js";

    public static final String EXTENSION_TEX_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/tex2jax.js";
    public static final String EXTENSION_TEX_FILE = "/extensions/tex2jax.js";

    public static final String MATH_EVENTS_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/MathEvents.js";
    public static final String MATH_EVENTS_FILE = "/extensions/MathEvents.js";

    public static final String TEX_AMS_MATH_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/AMSmath.js";
    public static final String TEX_AMS_MATH_FILE = "/extensions/TeX/AMSmath.js";

    public static final String TEX_AMS_SYMBOL_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/AMSsymbols.js";
    public static final String TEX_AMS_SYMBOL_FILE = "/extensions/TeX/AMSsymbols.js";

    public static final String TEX_AUTOLOAD_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/autoload-all.js";
    public static final String TEX_AUTOLOAD_FILE = "/extensions/TeX/autoload-all.js";

    public static final String TEX_CANCEL_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/cancel.js";
    public static final String TEX_CANCEL_FILE = "/extensions/TeX/cancel.js";

    public static final String TEX_COLOR_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/color.js";
    public static final String TEX_COLOR_FILE = "/extensions/TeX/color.js";

    public static final String JAX_ELEMENT_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/element/jax.js";
    public static final String JAX_ELEMENT_FILE = "/jax/element/mml/jax.js";

    public static final String JAX_INPUT_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/input/jax.js";
    public static final String JAX_INPUT_FILE = "/jax/input/TeX/jax.js";

    public static final String CONFIG_INPUT_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/input/config.js";
    public static final String CONFIG_INPUT_FILE = "/jax/input/TeX/config.js";

    public static final String MTABLE_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/output/mtable.js";
    public static final String MTABLE_FILE = "/jax/output/HTML-CSS/autoload/mtable.js";

    public static final String FONT_DATA_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/output/fontdata.js";
    public static final String FONT_DATA_FILE = "/jax/output/HTML-CSS/fonts/STIX/fontdata.js";

    public static final String FONT_DATA_1_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/output/fontdata-1.0.js";
    public static final String FONT_DATA_1_FILE = "/jax/output/HTML-CSS/fonts/STIX/fontdata-1.0.js";

    public static final String JAX_OUTPUT_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/output/jax.js";
    public static final String JAX_OUTPUT_FILE = "/jax/output/HTML-CSS/jax.js";

    public static final String CONFIG_OUTPUT_LINK = "/com/ustadmobile/lib/contentscrapers/ck12/mathjax/output/config.js";
    public static final String CONFIG_OUTPUT_FILE = "/jax/output/HTML-CSS/config.js";

    public static final String HINT_JSON_LINK = "/com/ustadmobile/lib/contentscrapers/khan/hint.json";
    public static final String HINT_JSON_FILE = "/hint.json";

    public static final String ATTEMPT_JSON_LINK = "/com/ustadmobile/lib/contentscrapers/khan/attempt.json";
    public static final String ATTEMPT_JSON_FILE = "/attempt.json";

    public static final String MATERIAL_JS = "materialize.min.js";
    public static final String MATERIAL_CSS = "materialize.min.css";

    public static final String brainGenieLink = "braingenie.ck12.org";
    public static final String slideShareLink = "www.slideshare.net";

    public static final String MODULE_TIN_CAN_FILE = "http://adlnet.gov/expapi/activities/module";
    public static final String SIMULATION_TIN_CAN_FILE = "http://adlnet.gov/expapi/activities/simulation";
    public static final String VIDEO_TIN_CAN_FILE = "http://activitystrea.ms/schema/1.0/video";
    public static final String ARTICLE_TIN_CAN_FILE = "http://activitystrea.ms/schema/1.0/ARTICLE_TIN_CAN_FILE";
    public static final String ASSESMENT_TIN_CAN_FILE = "http://adlnet.gov/expapi/activities/assessment";

    public static final List<String> QUESTION_SET_HOLDER_TYPES = Arrays.asList(
           ComponentType.EXCERCISE.getType(), ComponentType.ONLINE.getType(),
           ComponentType.TEST.getType());



    public static final String ARABIC_FONT_REGULAR = "DroidNaskh-Regular.woff2";
    public static final String ARABIC_FONT_BOLD = "DroidNaskh-Bold.woiff2";
    public static final String INDEX_HTML = "index.html";
    public static final String JQUERY_JS = "jquery-3.3.1.min.js";

    public static final String MIMETYPE_ZIP = "application/zip";
    public static final String MIMETYPE_EPUB = "application/epub+zip";
    public static final String MIMETYPE_JSON = "application/json";
    public static final String MIMETYPE_SVG = "image/svg+xml";
    public static final String MIMETYPE_JPG = "image/jpg";

    public static final String ZIP_EXT = ".zip";
    public static final String PNG_EXT = ".png";
    public static final String EPUB_EXT = ".epub";
    public static final String SVG_EXT = ".svg";
    public static final String JSON_EXT = ".json";

    public static final String ARABIC_LANG_CODE = "ar";
    public static final String ENGLISH_LANG_CODE = "en";
    public static final String USTAD_MOBILE = "Ustad Mobile";
    public static final String ROOT = "root";
    public static final String EMPTY_STRING = "";


    public enum QUESTION_TYPE{

        MULTI_CHOICE("multiple-choice"),
        FILL_BLANKS("fill-in-the-blanks"),
        SHORT_ANSWER("short-answer");

        private String type;

        QUESTION_TYPE(String questionType) {
            this.type = questionType;
        }

        public String getType() {
            return type;
        }

    }


    public enum ComponentType{
        MAIN("MainContentTrack"),
        SECTION("Section"),
        SUBSECTION("SubSection"),
        IMPORTED("ImportedComponent"),
        MULTICHOICE("MultipleChoiceQuestion"),
        QUESTIONSET("QuestionSet"),
        TEST("Test"),
        VIDEO("Video"),
        EXCERCISE("Exercise"),
        NUMERIC_QUESTION("NumericResponseQuestion"),
        ONLINE("OnlineLesson");

        private String type;

        ComponentType(String compType) {
            this.type = compType;
        }

        public String getType() {
            return type;
        }
    }

    public enum HtmlName{
        DESC("description"),
        FULL_DESC("full_description"),
        EXPLAIN("explaination"),
        CHOICE("choice"),
        HINT("hint");

        private String name;

        HtmlName(String compType) {
            this.name = compType;
        }

        public String getName() {
            return name;
        }

    }

}
