import com.gargoylesoftware.css.parser.CSSErrorHandler;
import com.gargoylesoftware.css.parser.CSSException;
import com.gargoylesoftware.css.parser.CSSParseException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.parser.HTMLParserListener;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Main {

    static String classroomName = "";
    static String lectureName = "";

    public static void disableLogger(WebClient webClient) {
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

        webClient.setIncorrectnessListener((arg0, arg1) -> {
        });
        webClient.setCssErrorHandler(new CSSErrorHandler() {

            @Override
            public void warning(CSSParseException exception) throws CSSException {
            }

            @Override
            public void error(CSSParseException exception) throws CSSException {
            }

            @Override
            public void fatalError(CSSParseException exception) throws CSSException {
            }
        });
        webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

            @Override
            public void scriptException(HtmlPage page, com.gargoylesoftware.htmlunit.ScriptException scriptException) {
            }

            @Override
            public void timeoutError(HtmlPage arg0, long arg1, long arg2) {
            }

            @Override
            public void malformedScriptURL(HtmlPage arg0, String arg1, MalformedURLException arg2) {
            }

            @Override
            public void loadScriptError(HtmlPage arg0, URL arg1, Exception arg2) {
            }

            @Override
            public void warn(String message, String sourceName, int line, String lineSource, int lineOffset) {
            }
        });
        webClient.setHTMLParserListener(new HTMLParserListener() {

            @Override
            public void error(String message, URL url, String html, int line, int column, String key) {
            }

            @Override
            public void warning(String message, URL url, String html, int line, int column, String key) {
            }
        });

        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
    }

    public static Connection.Response GET(String URL, Map<String, String> cookies, String schoolLink, String[]... headers) throws IOException {

        Map<String, String> headersMap = new HashMap<>();
        if (headers.length != 0)
            Arrays.asList(headers).forEach((ArrayHeader) -> headersMap.put(ArrayHeader[0], ArrayHeader[1]));

        return Jsoup.connect(URL)
                .timeout(3000)
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Cache-Control", "max-age=0")
                .header("Connection", "keep-alive")
                .header("Host", schoolLink.replace("https://", "").replace("/", ""))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:80.0) Gecko/20100101 Firefox/80.0")
                .headers(headersMap)
                .cookies(cookies)
                .method(Connection.Method.GET)
                .ignoreContentType(true)
                .execute();
    }

    public static Connection.Response POST(String URL, Map<String, String> cookies, String schoolLink, Map<String, String> data, String[]... headers) throws IOException {

        Map<String, String> headersMap = new HashMap<>();
        if (headers.length != 0)
            Arrays.asList(headers).forEach((ArrayHeader) -> headersMap.put(ArrayHeader[0], ArrayHeader[1]));

        return Jsoup.connect(URL)
                .timeout(3000)
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Cache-Control", "max-age=0")
                .header("Connection", "keep-alive")
                .header("Host", schoolLink.replace("https://", "").replace("/", ""))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:80.0) Gecko/20100101 Firefox/80.0")
                .headers(headersMap)
                .cookies(cookies)
                .data(data)
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();
    }

    public static JSONObject findSchool(Scanner scanner, boolean isTestMode) throws ParseException, IOException, ScriptException {

        String area, schoolName;
        if (isTestMode) {
            area = "용산구";
            schoolName = "선린인터넷고등학교";
        } else {
            System.out.print("Please enter the school area: ");
            area = scanner.nextLine().trim();
            System.out.print("Please enter the name of the school: ");
            schoolName = scanner.nextLine().trim();
        }

        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
        ScriptContext context = engine.getContext();
        StringWriter writer = new StringWriter();
        context.setWriter(writer);

        engine.eval(Jsoup.connect("https://oc.ebssw.kr/resource/schoolList.js").header("Accept", "*/*").header("User-Agent", "EBS 병신").ignoreContentType(true).execute().body() + "print(JSON.stringify(schulJSONObj))");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonRootObject = (JSONObject) jsonParser.parse(writer.toString());
        JSONArray jsonSchoolArray = (JSONArray) jsonRootObject.get("schulList");

        for (Object jsonObject : jsonSchoolArray) {
            JSONObject jsonSchool = (JSONObject) jsonObject;
            if (jsonSchool.get("schulNm").equals(schoolName) && jsonSchool.get("areaNm").equals(area)) {
                System.out.println("\nFound the school: https://" + jsonSchool.get("host") + ".ebssw.kr/");
                return jsonSchool;
            }
        }

        System.out.println("\nCannot found the school - " + area + "_" + schoolName + ", Exit.");
        return null;
    }

    public static Map<String, String> login(Scanner scanner, String schoolLink, boolean isTestMode) throws IOException, ParseException {

        String ID, PW;
        if (isTestMode) {
            ID = "hurrhnn04";
            PW = Files.readAllLines(Paths.get("TESTMODE_PW")).get(0);
        } else {
            System.out.print("ID: ");
            ID = scanner.nextLine().trim();

            System.out.print("PW: ");
            PW = scanner.nextLine().trim();
        }

        Connection.Response loginPageResponse = GET(schoolLink + "sso/loginView.do?loginType=onlineClass", new HashMap<>(), schoolLink, new String[]{"Referer", schoolLink + "onlineClass/search/onlineClassSearchView.do"});
        Map<String, String> loginTryCookie = loginPageResponse.cookies();
        Document loginPageDocument = loginPageResponse.parse();

        Map<String, String> postData = new HashMap<>();
        postData.put("j_username", ID);
        postData.put("j_password", PW);


        Connection.Response loginPageRequest = Jsoup.connect(schoolLink + "sso/checkGroupMber.do")
                .timeout(3000)
                .header("Accept", "*/*")
                .header("AJAX", "true")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Connection", "keep-alive")
                .header("Host", schoolLink.replace("https://", "").replace("/", ""))
                .header("Referer", schoolLink + "sso/loginView.do?loginType=onlineClass")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:80.0) Gecko/20100101 Firefox/80.0")
                .header("X-Requested-With", "XMLHttpRequest")
                .cookies(loginTryCookie)
                .data(postData)
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(loginPageRequest.body());
        String parseResult = (String) jsonObject.get("result");

        System.out.println("\nResponse: " + loginPageRequest.statusCode() + " " + loginPageRequest.statusMessage());
        if (parseResult.equals("noUser")) {
            System.out.println("ID or PW is incorrect. Exit.");
            System.exit(0);
        } else if (parseResult.equals("overId"))
            System.out.println("Login session already exists.\nProceed anyway.");

        postData.put("c", loginPageDocument.select("#c").val());
        postData.put("SAMLRequest", loginPageDocument.select("#SAMLRequest").val());
        postData.put("j_returnurl", loginPageDocument.select("#j_returnurl").val());
        postData.put("j_loginurl", loginPageDocument.select("#j_loginurl").val());
        postData.put("j_logintype", loginPageDocument.select("#j_logintype").val());
        postData.put("localLoginUrl", loginPageDocument.select("#localLoginUrl").val());
        postData.put("hmpgId", loginPageDocument.select("#hmpgId").val());
        postData.put("userSeCode", loginPageDocument.select("#userSeCode").val());
        postData.put("loginType", loginPageDocument.select("#loginType").val());

        loginPageRequest = Jsoup.connect(schoolLink + "sso")
                .timeout(3000)
                .header("Accept", "*/*")
                .header("AJAX", "true")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3")
                .header("Connection", "keep-alive")
                .header("Cookie", "sso.authenticated=0")
                .header("Host", schoolLink.replace("https://", "").replace("/", ""))
                .header("Referer", schoolLink + "sso/loginView.do?loginType=onlineClass")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:80.0) Gecko/20100101 Firefox/80.0")
                .header("X-Requested-With", "XMLHttpRequest")
                .cookies(loginTryCookie)
                .data(postData)
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();

        System.out.println("\nLogin Successful!");
        return loginPageRequest.cookies();
    }

    public static Elements parseClassroomList(Map<String, String> sessionCookie, JSONObject jsonSchool, String schoolLink) throws IOException {
        Map<String, String> postData = new HashMap<>();
        postData.put("searchKeyword", (String) jsonSchool.get("schulCcode"));
        postData.put("filterList", "");

        Connection.Response getCookie = GET(schoolLink + "onlineClass/search/onlineClassSearchView.do?schulCcode=" + jsonSchool.get("schulCcode"), sessionCookie, schoolLink, new String[]{"Referer", schoolLink + "onlineClass/search/onlineClassSearchView.do"});
        Map<String, String> additionalCookie = getCookie.cookies();

        Map<String, String> sumCookie = new HashMap<>();
        sumCookie.putAll(sessionCookie);
        sumCookie.putAll(additionalCookie);

        Connection.Response response = POST(schoolLink + "onlineClass/search/onlineClassSearch.do", sumCookie, schoolLink, postData);
        return response.parse().select("a.txt_violet");
    }

    public static Document parseClassroomPage(Scanner scanner, Map<String, String> sessionCookie, JSONObject jsonSchool, String schoolLink) throws IOException {
        System.out.println("Please select a class room to enter. \n");
        Elements classroomList = parseClassroomList(sessionCookie, jsonSchool, schoolLink);
        Object[] classroomArr = classroomList.eachAttr("title").toArray();
        for (int i = 0; i < classroomArr.length; i++)
            System.out.println((i + 1) + ". " + classroomArr[i]);

        System.out.print("\nClassroom> ");
        int classNumber = scanner.nextInt();

        classroomName = classroomArr[classNumber - 1].toString();
        Connection.Response classroomPage = GET(schoolLink + classroomList.eachAttr("href").get(classNumber - 1), sessionCookie, schoolLink, new String[]{"Referer", schoolLink + "onlineClass/search/onlineClassSearchView.do"});
        return classroomPage.parse();
    }

    public static String parseSelectLecturePage(Scanner scanner, Map<String, String> sessionCookie, JSONObject jsonSchool, String schoolLink) throws IOException, URISyntaxException {
        Document classroomPage = parseClassroomPage(scanner, sessionCookie, jsonSchool, schoolLink);

        Object[] selectLecturePage = new Object[classroomPage.select("a.link_area").size()];
        String[] lectureNameArray = new String[classroomPage.select("a.link_area").size()];
        int cnt = 0;
        for (Element link_area : classroomPage.select("a.link_area")) {
            for (Element element : link_area.getElementsByClass("tit bold")) {
                lectureNameArray[cnt] = element.text();
                System.out.println(++cnt + ". " + element.text());
                selectLecturePage[cnt - 1] = schoolLink + (link_area.attr("href").substring(1));
            }
        }
        System.out.print("\nLecture> ");
        int lectureNumber = scanner.nextInt();

        System.out.print("Parsing... please wait.");
        lectureName = lectureNameArray[lectureNumber - 1];

        URL lectureSite = new URI(selectLecturePage[lectureNumber - 1].toString()).toURL();
        WebClient webClient = new WebClient();
        disableLogger(webClient);

        webClient.addCookie("JSESSIONID=" + sessionCookie.get("JSESSIONID"), lectureSite, null);
        webClient.addCookie("LSCO=" + sessionCookie.get("LSCO"), lectureSite, null);
        webClient.getOptions().setThrowExceptionOnScriptError(false);

        HtmlPage LecturePage = webClient.getPage(lectureSite);
        return LecturePage.asXml();
    }

    public static List<String> parseLecturePage(String selectLecturePage, Map<String, String> sessionCookie, String schoolLink) throws IOException {
        List<String> videosInfo = new LinkedList<>();

        int atnlcNo = Integer.parseInt(Jsoup.parse(selectLecturePage).select("#atnlcNo").val());
        int stepSn = Integer.parseInt(Jsoup.parse(selectLecturePage).select("#stepSn").val());

        Connection.Response lecturePage = GET(schoolLink + "mypage/userlrn/userLrnView.do?atnlcNo=" + atnlcNo + "&stepSn=" + stepSn + "&onlineClassYn=Y", sessionCookie, schoolLink);

        Elements lectures = lecturePage.parse().getElementsByClass("on");
        String[] lecturesHref = new String[lectures.size()];
        for (int i = 0; i < lectures.size(); i++) {
//            StringBuilder spLine = new StringBuilder();
//            for (int j = 0; j < lectures.get(i).getElementsByClass("txt").text().length(); j++) spLine.append("= ");
//            System.out.println(lectures.get(i).getElementsByClass("txt").toString().contains("<strong") ? "\n" + spLine.toString() + "\n" + lectures.get(i).getElementsByClass("txt").text() + "\n" + spLine.toString() : lectures.get(i).getElementsByClass("txt").text());
            lecturesHref[i] = lectures.get(i).attr("href");
        }

        System.out.print("\n");

        for (int i = 0; i < lectures.size(); i++) {
            Map<String, String> data = new HashMap<>();
            data.put("stepSn", Integer.toString(stepSn));
            data.put("sessSn", "");
            data.put("atnlcNo", Integer.toString(atnlcNo));

            String videoScriptString = "";
            try {
                int endIndex = lecturesHref[i].replace("javascript:loadCntntsLeft( '", "").indexOf('\'');
                data.put("lctreSn", lecturesHref[i].replace("javascript:loadCntntsLeft( '", "").substring(0, endIndex));

                String replace = lecturesHref[i].replace("javascript:loadCntntsLeft( '", "").substring(endIndex).replace("', '", "");
                data.put("cntntsTyCode", replace.substring(0, replace.indexOf('\'')));

                Connection.Response lectureResponse = POST(schoolLink + "mypage/userlrn/userLrnMvpView.do", sessionCookie, schoolLink, data, new String[]{"Referer", lecturePage.url().toString()});
                videoScriptString = lectureResponse.body();
                if (lectureResponse.body().contains("youtube.com")) throw new NullPointerException();

                String parseData = videoScriptString.substring(videoScriptString.indexOf("//동영상 리스트 JSON 데이터"), videoScriptString.indexOf("//북마크 리스트 JSON 데이터")).replace("//동영상 리스트 JSON 데이터", "");
                parseData = parseData.substring(parseData.indexOf("mvpListJsonArray = JSON.parse( "), parseData.indexOf(" );"));

                ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
                ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
                ScriptContext context = engine.getContext();
                StringWriter writer = new StringWriter();
                context.setWriter(writer);

                parseData = "var " + parseData + " )\nprint(JSON.stringify(mvpListJsonArray))";
                parseData = parseData.replace("JSON.parse(", "JSON.stringify(").replace("'[", "[").replace("]'", ",]");

                engine.eval(parseData);
                String resultString = writer.toString();
                resultString = resultString.replace("\\", "").replace("\r\n", "");
                resultString = resultString.substring(1, (resultString.length() - 1));

                JSONArray jsonRootArray = (JSONArray) new JSONParser().parse(resultString);
                JSONObject jsonVideoObject;

                if (jsonRootArray.size() > 1)
                    jsonVideoObject = (JSONObject) jsonRootArray.get(1);
                else jsonVideoObject = (JSONObject) jsonRootArray.get(0);

                String parsedLink = !(jsonVideoObject.get("src").toString().contains("http://")) ? "" : lectures.get(i).getElementsByClass("txt").text() + "\u200B" + jsonVideoObject.get("src").toString();
                if (!(parsedLink.equals("") || parsedLink.isEmpty())) videosInfo.add(parsedLink);
            } catch (NullPointerException e) {

                String parsedLink = lectures.get(i).getElementsByClass("txt").text() + "\u200B" + videoScriptString.substring(videoScriptString.indexOf("https://www.youtube.com"), videoScriptString.indexOf("?enablejsapi=1&rel=0&autoplay=1\"")).replace("embed/", "watch?v=");
                videosInfo.add(parsedLink);
            } catch (Exception ignored) {
            }
        }

        return videosInfo;
    }

    public static String detectOS() {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win"))
            return "Windows";
        else if (OS.contains("mac"))
            return "Mac";
        else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"))
            return "Unix";
        else if (OS.contains("linux"))
            return "Linux";
        else if (OS.contains("sunos"))
            return "Solaris";
        else return "";
    }

    public static boolean downloadFile(String URL, String filePath) {
        try {
            ReadableByteChannel readChannel;
            readChannel = Channels.newChannel(new URL(URL).openStream());

            FileOutputStream fileOS = new FileOutputStream(filePath);
            FileChannel writeChannel = fileOS.getChannel();
            writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);

            readChannel.close();
            fileOS.close();
            writeChannel.close();
            return true;
        } catch (Exception ignored) {
            new File(filePath).deleteOnExit();
            return false;
        }
    }

    public static void main(String[] args) throws IOException, ParseException, ScriptException, URISyntaxException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        boolean isTestMode = false;
        if (args.length > 0)
            if (args[0].equals("TESTMODE=1")) isTestMode = true;

        JSONObject jsonSchool = findSchool(scanner, isTestMode);
        if (jsonSchool == null)
            return;

        String schoolLink = "https://" + jsonSchool.get("host") + ".ebssw.kr/";
        Map<String, String> sessionCookie = login(scanner, schoolLink, isTestMode);

        String selectLecturePage = parseSelectLecturePage(scanner, sessionCookie, jsonSchool, schoolLink);
        Jsoup.parse(selectLecturePage);

        List<String> videosInfo = parseLecturePage(selectLecturePage, sessionCookie, schoolLink);

        String tmpFolderPath = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "youtube-dl";
        File tmpFolder = new File(tmpFolderPath);
        if (!tmpFolder.exists()) {
            if (tmpFolder.mkdirs()) {
                System.out.println("Making temporary folder for download modules - " + tmpFolderPath);
            } else {
                System.out.println("Cannot making temporary folder for download modules.. Check your permissions.");
                return;
            }
        }

        System.out.println("Downloading modules at " + tmpFolderPath);
        if (!new File(tmpFolderPath + System.getProperty("file.separator") + (detectOS().equals("Windows") ? "ffmpeg.exe" : "ffmpeg")).exists()) {
            if (!downloadFile(detectOS().equals("Windows") ? "http://hurrhnn.xyz/!php/ffmpeg/ffmpeg.exe" : "http://hurrhnn.xyz/!php/ffmpeg/ffmpeg", tmpFolderPath + System.getProperty("file.separator") + (detectOS().equals("Windows") ? "ffmpeg.exe" : "ffmpeg"))) {
                System.out.println("Download Failed - FFmpeg, Exit.");
                return;
            }
        } else System.out.println("FFmpeg already downloaded.");

        if (!new File(tmpFolderPath + System.getProperty("file.separator") + (detectOS().equals("Windows") ? "youtube-dl.exe" : "youtube-dl")).exists()) {
            if (!downloadFile(detectOS().equals("Windows") ? "http://hurrhnn.xyz/!php/youtube-dl/youtube-dl.exe" : "http://hurrhnn.xyz/!php/youtube-dl/youtube-dl", tmpFolderPath + System.getProperty("file.separator") + (detectOS().equals("Windows") ? "youtube-dl.exe" : "youtube-dl"))) {
                System.out.println("Download Failed - youtube-dl, Exit.");
                return;
            }
        } else System.out.println("youtube-dl already downloaded.");

        int cnt = 0;
        System.out.println("\nStarting download videos.");
        for (String videoInfo : videosInfo) {
            File downloadFolder = new File(classroomName + System.getProperty("file.separator") + lectureName);
            if (!downloadFolder.exists()) {
                if (downloadFolder.mkdirs())
                    onDownloadReady(tmpFolderPath + System.getProperty("file.separator") + (detectOS().equals("Windows") ? "youtube-dl.exe" : "youtube-dl"), videoInfo, downloadFolder, ++cnt);
            } else onDownloadReady(tmpFolderPath + System.getProperty("file.separator") + (detectOS().equals("Windows") ? "youtube-dl.exe" : "youtube-dl"), videoInfo, downloadFolder, ++cnt);
        }

        System.out.println("Downloaded all downloadable videos!");
    }

    public static File[] listFilesMatching(File fileName, String regexString) {
        final Pattern p = Pattern.compile(regexString);
        return fileName.listFiles(file -> p.matcher(file.getName()).matches());
    }

    public static void onDownloadReady(String tmpFolderPath, String videoInfo, File downloadFolder, int videoCount) throws IOException, InterruptedException {
        String videoName = videoInfo.substring(0, videoInfo.indexOf("\u200B"));
        String videoURL = videoInfo.substring(videoInfo.indexOf("\u200B")).replace("\u200B", "");

        if (videoURL.contains("youtube.com")) {
            File chkFile = new File(downloadFolder.getPath() + System.getProperty("file.separator") + "chk_file");
            if(chkFile.createNewFile()) {
                for(File file : Objects.requireNonNull(downloadFolder.listFiles())) {
                    if(file.getName().trim().contains(new File(videoName).getName().trim()) || new File(videoName).getName().trim().contains(file.getName().trim())) {
                        System.out.println(videoCount + ". Exists video. Skip download - " + videoName);
                        chkFile.delete();
                        return;
                    }
                }
            }
            chkFile.delete();

            String[] command = new String[] {new File(tmpFolderPath).getPath(), "--user-agent", "\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.75 Safari/537.36\"", "-f", "best", "-o", "downloading.%(ext)s", videoURL};
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = outReader.readLine()) != null) {
                System.out.print(line + "\r");
            }
            while ((line = errorReader.readLine()) != null) {
                System.out.println(line + "\r");
            }

            Path relativePath = Paths.get("");
            File[] files = listFilesMatching(new File(relativePath.toAbsolutePath().toString()), "downloading.*");

            File file = (File) Arrays.stream(files).toArray()[0];
            File renameTo = new File( downloadFolder.getPath() + System.getProperty("file.separator") + videoName.replaceAll("[\\\\:*?\"<>|]", "") + file.getName().substring(file.getName().indexOf(".")));
            if(file.exists()) {
                if (file.renameTo(renameTo)) {
                    System.out.print(videoCount + ". Downloading at \"" + downloadFolder.getPath() + System.getProperty("file.separator") + renameTo.getName());
                    System.out.println(" - Done!");
                } else System.out.println(file.getName() + ", " + renameTo.getName());
            }
            Thread.sleep(1000);
        } else {
            videoName += ".mp4";
            File videoFile = new File(downloadFolder.getPath() + System.getProperty("file.separator") + videoName);
            if (videoFile.createNewFile()) {
                System.out.print(videoCount + ". Downloading at \"" + downloadFolder.getPath() + System.getProperty("file.separator") + videoName + "\"");
                downloadFile(videoURL, downloadFolder.getPath() + System.getProperty("file.separator") + videoName);
                System.out.println(" - Done!");
            } else System.out.println(videoCount + ". Exists video. Skip download - " + videoName.replace(".mp4", ""));
        }
    }
}