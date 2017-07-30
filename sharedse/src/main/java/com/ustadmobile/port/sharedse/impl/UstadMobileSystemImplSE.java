/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ustadmobile.port.sharedse.impl;

import com.ustadmobile.core.controller.CatalogController;
import com.ustadmobile.core.generated.locale.MessageID;
import com.ustadmobile.core.impl.HTTPResult;
import com.ustadmobile.core.impl.TinCanQueueListener;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UMStorageDir;
import com.ustadmobile.core.impl.UstadMobileConstants;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.model.CourseProgress;
import com.ustadmobile.core.util.Base64Coder;
import com.ustadmobile.core.util.UMFileUtil;

import com.ustadmobile.core.impl.ZipFileHandle;
import com.ustadmobile.core.util.UMIOUtils;
import com.ustadmobile.core.util.UMTinCanUtil;
import com.ustadmobile.core.util.UMUUID;
import com.ustadmobile.nanolrs.core.endpoints.XapiAgentEndpoint;
import com.ustadmobile.nanolrs.core.manager.UserCustomFieldsManager;
import com.ustadmobile.nanolrs.core.manager.UserManager;
import com.ustadmobile.nanolrs.core.manager.XapiStatementManager;
import com.ustadmobile.nanolrs.core.model.User;
import com.ustadmobile.nanolrs.core.model.XapiAgent;
import com.ustadmobile.nanolrs.core.model.XapiStatement;
import com.ustadmobile.nanolrs.core.persistence.PersistenceManager;
import com.ustadmobile.port.sharedse.impl.zip.*;
import com.ustadmobile.port.sharedse.networkmanager.NetworkManager;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 *
 * @author mike
 */
public abstract class UstadMobileSystemImplSE extends UstadMobileSystemImpl {

    private XmlPullParserFactory xmlPullParserFactory;

    protected XapiAgent xapiAgent;

    /**
     * Convenience method to return a casted instance of UstadMobileSystemImplSharedSE
     *
     * @return Casted UstadMobileSystemImplSharedSE
     */
    public static UstadMobileSystemImplSE getInstanceSE() {
        return (UstadMobileSystemImplSE)UstadMobileSystemImpl.getInstance();
    }

    /**
     * @inheritDoc
     */
    @Override
    public HTTPResult makeRequest(String httpURL, Hashtable headers, Hashtable postParams, String method, byte[] postBody) throws IOException {
        URL url = new URL(httpURL);
        HttpURLConnection conn = (HttpURLConnection)openConnection(url);

        if(headers != null) {
            Enumeration e = headers.keys();
            while(e.hasMoreElements()) {
                String headerField = e.nextElement().toString();
                String headerValue = headers.get(headerField).toString();
                conn.setRequestProperty(headerField, headerValue);
            }
        }
        //conn.setRequestProperty("Connection", "close");

        conn.setRequestMethod(method);

        if("POST".equals(method)) {
            if(postBody == null && postParams != null && postParams.size() > 0) {
                //we need to write the post params to the request
                StringBuilder sb = new StringBuilder();
                Enumeration e = postParams.keys();
                boolean firstParam = true;
                while(e.hasMoreElements()) {
                    String key = e.nextElement().toString();
                    String value = postParams.get(key).toString();
                    if(firstParam) {
                        firstParam = false;
                    }else {
                        sb.append('&');
                    }
                    sb.append(URLEncoder.encode(key, "UTF-8")).append('=');
                    sb.append(URLEncoder.encode(value, "UTF-8"));
                }

                postBody = sb.toString().getBytes();
            }else if(postBody == null) {
                throw new IllegalArgumentException("Cant make a post request with no body and no parameters");
            }

            conn.setDoOutput(true);
            OutputStream out = conn.getOutputStream();
            out.write(postBody);
            out.flush();
            out.close();
        }

        conn.connect();

        int contentLen = conn.getContentLength();
        int statusCode = conn.getResponseCode();
        InputStream in = statusCode < 400 ? conn.getInputStream() : conn.getErrorStream();
        byte[] buf = new byte[1024];
        int bytesRead = 0;
        int bytesReadTotal = 0;

        //do not read more bytes than is available in the stream
        //TODO: The above will be wrong when gzip is in use
        int bytesToRead = Math.min(buf.length, contentLen != -1 ? contentLen : buf.length);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if(!method.equalsIgnoreCase("HEAD")) {
            while((contentLen != -1 ? (bytesRead < contentLen) : true)  && (bytesRead = in.read(buf, 0, contentLen == -1 ? buf.length : Math.min(buf.length, contentLen - bytesRead))) != -1) {
                bout.write(buf, 0, bytesRead);
            }
        }

        UMIOUtils.closeInputStream(in);

        Hashtable responseHeaders = new Hashtable();
        Iterator<String> headerIterator = conn.getHeaderFields().keySet().iterator();
        while(headerIterator.hasNext()) {
            String header = headerIterator.next();
            if(header == null) {
                continue;//a null header is the response line not header; leave that alone...
            }

            String headerVal = conn.getHeaderField(header);
            responseHeaders.put(header.toLowerCase(), headerVal);
        }

        byte[] resultBytes = bout.toByteArray();
        HTTPResult result = new HTTPResult(resultBytes, statusCode,
                responseHeaders);

        return result;
    }

    /**
     * Open the given connection and return the HttpURLConnection object using a proxy if required
     *
     * @param url
     *
     * @return
     */
    public abstract URLConnection openConnection(URL url) throws IOException;

    @Override
    public boolean isJavascriptSupported() {
        return true;
    }

    @Override
    public boolean isHttpsSupported() {
        return true;
    }

    @Override
    public boolean queueTinCanStatement(final JSONObject stmt, final Object context) {
        //Placeholder for iOS usage
        return false;
    }

    public void addTinCanQueueStatusListener(final TinCanQueueListener listener) {
        //TODO: remove this - it's not really used - do nothing
    }

    public void removeTinCanQueueListener(TinCanQueueListener listener) {
        //TODO: remove this - it's not really used - do nothing
    }

    /**
     * Returns the system base directory to work from
     *
     * @return
     */
    protected abstract String getSystemBaseDir(Object context);


    @Override
    public String getCacheDir(int mode, Object context) {
        String systemBaseDir = getSystemBaseDir(context);
        if(mode == CatalogController.SHARED_RESOURCE) {
            return UMFileUtil.joinPaths(new String[]{systemBaseDir, UstadMobileConstants.CACHEDIR});
        }else {
            return UMFileUtil.joinPaths(new String[]{systemBaseDir, "user-" + getActiveUser(context),
                    UstadMobileConstants.CACHEDIR});
        }
    }

    @Override
    public UMStorageDir[] getStorageDirs(int mode, Object context) {
        List<UMStorageDir> dirList = new ArrayList<>();
        String systemBaseDir = getSystemBaseDir(context);

        if((mode & CatalogController.SHARED_RESOURCE) == CatalogController.SHARED_RESOURCE) {
            dirList.add(new UMStorageDir(systemBaseDir, getString(MessageID.device, context),
                    false, true, false));

            //Find external directories
            String[] externalDirs = findRemovableStorage();
            for(String extDir : externalDirs) {
                dirList.add(new UMStorageDir(UMFileUtil.joinPaths(new String[]{extDir,
                        UstadMobileSystemImpl.CONTENT_DIR_NAME}),
                        getString(MessageID.memory_card, context),
                        true, true, false, false));
            }
        }

        if((mode & CatalogController.USER_RESOURCE) == CatalogController.USER_RESOURCE) {
            String userBase = UMFileUtil.joinPaths(new String[]{systemBaseDir, "user-"
                    + getActiveUser(context)});
            dirList.add(new UMStorageDir(userBase, getString(MessageID.device, context),
                    false, true, true));
        }




        UMStorageDir[] retVal = new UMStorageDir[dirList.size()];
        dirList.toArray(retVal);
        return retVal;
    }

    /**
     * Provides a list of paths to removable stoage (e.g. sd card) directories
     *
     * @return
     */
    public String[] findRemovableStorage() {
        return new String[0];
    }

    /**
     * Will return language_COUNTRY e.g. en_US
     *
     * @return
     */
    @Override
    public String getSystemLocale(Object context) {
        return Locale.getDefault().toString();
    }


    @Override
    public long fileLastModified(String fileURI) {
        return new File(fileURI).lastModified();
    }

    @Override
    public OutputStream openFileOutputStream(String fileURI, int flags) throws IOException {
        fileURI = UMFileUtil.stripPrefixIfPresent("file://", fileURI);
        return new FileOutputStream(fileURI, (flags & FILE_APPEND) == FILE_APPEND);
    }

    @Override
    public InputStream openFileInputStream(String fileURI) throws IOException {
        fileURI = UMFileUtil.stripPrefixIfPresent("file://", fileURI);
        return new FileInputStream(fileURI);
    }


    @Override
    public boolean fileExists(String fileURI) throws IOException {
        fileURI = UMFileUtil.stripPrefixIfPresent("file://", fileURI);
        return new File(fileURI).exists();
    }

    @Override
    public boolean dirExists(String dirURI) throws IOException {
        dirURI = UMFileUtil.stripPrefixIfPresent("file://", dirURI);
        File dir = new File(dirURI);
        return dir.exists() && dir.isDirectory();
    }

    @Override
    public boolean removeFile(String fileURI)  {
        fileURI = UMFileUtil.stripPrefixIfPresent("file://", fileURI);
        File f = new File(fileURI);
        return f.delete();
    }

    @Override
    public String[] listDirectory(String dirURI) throws IOException {
        dirURI = UMFileUtil.stripPrefixIfPresent("file://", dirURI);
        File dir = new File(dirURI);
        return dir.list();
    }


    @Override
    public boolean renameFile(String path1, String path2) {
        File file1 = new File(path1);
        File file2 = new File(path2);
        return file1.renameTo(file2);
    }

    @Override
    public long fileSize(String path) {
        File file = new File(path);
        return file.length();
    }

    @Override
    public long fileAvailableSize(String fileURI) throws IOException {
        return new File(fileURI).getFreeSpace();
    }

    @Override
    public boolean makeDirectory(String dirPath) throws IOException {
        File newDir = new File(dirPath);
        return newDir.mkdir();
    }

    @Override
    public boolean makeDirectoryRecursive(String dirURI) throws IOException {
        return new File(dirURI).mkdirs();
    }

    @Override
    public boolean removeRecursively(String path) {
        return removeRecursively(new File(path));
    }

    public boolean removeRecursively(File f) {
        if(f.isDirectory()) {
            File[] dirContents = f.listFiles();
            for(int i = 0; i < dirContents.length; i++) {
                if(dirContents[i].isDirectory()) {
                    removeRecursively(dirContents[i]);
                }
                dirContents[i].delete();
            }
        }
        return f.delete();
    }

    public XmlPullParser newPullParser() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        return parser;
    }

    public XmlSerializer newXMLSerializer() {
        XmlSerializer serializer = null;
        try {
            if(xmlPullParserFactory == null) {
                xmlPullParserFactory = XmlPullParserFactory.newInstance();
            }

            serializer = xmlPullParserFactory.newSerializer();
        }catch(XmlPullParserException e) {
            l(UMLog.ERROR, 92, null, e);
        }

        return serializer;
    }

    /**
     * @inheritDoc
     */
    @Override
    public ZipFileHandle openZip(String name) throws IOException{
        return new ZipFileHandleSharedSE(name);
    }

    /**
     * @{inheritDoc}
     */
    public String hashAuth(Object context, String auth) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(auth.getBytes());
            byte[] digest = md.digest();
            return new String(Base64Coder.encode(digest));
        }catch(NoSuchAlgorithmException e) {
            l(UMLog.ERROR, 86, null, e);
        }

        return null;
    }

    /**
     * Return the network manager for this platform
     *
     * @return
     */
    public abstract NetworkManager getNetworkManager();

    protected XapiAgent getCurrentAgent() {
        //This is set with setActiveUser
        return xapiAgent;
    }

    @Override
    public void setActiveUser(String username, Object context) {
        super.setActiveUser(username, context);
        xapiAgent = username != null ? XapiAgentEndpoint.createOrUpdate(context, null, username,
                UMTinCanUtil.getXapiServer(context)) : null;
    }

    @Override
    public CourseProgress getCourseProgress(String[] entryIds, Object context) {
        if(getActiveUser(context) == null)
            return null;

        XapiStatementManager stmtManager = PersistenceManager.getInstance().getManager(XapiStatementManager.class);

        String[] entryIdsPrefixed = new String[entryIds.length];
        for(int i = 0; i < entryIdsPrefixed.length; i++) {
            entryIdsPrefixed[i] = "epub:" + entryIds[i];
        }

        List<? extends XapiStatement> progressStmts = stmtManager.findByProgress(context,
                entryIdsPrefixed, getCurrentAgent(), null, new String[]{
                    UMTinCanUtil.VERB_ANSWERED, UMTinCanUtil.VERB_PASSED, UMTinCanUtil.VERB_FAILED
                }, 1);

        if(progressStmts.size() == 0) {
            return new CourseProgress(CourseProgress.STATUS_NOT_STARTED, 0, 0);
        }else {
            XapiStatement stmt = progressStmts.get(0);
            String stmtVerb = stmt.getVerb().getVerbId();
            CourseProgress courseProgress = new CourseProgress();
            if(stmtVerb.equals(UMTinCanUtil.VERB_ANSWERED))
                courseProgress.setStatus(MessageID.in_progress);
            else if(stmtVerb.equals(UMTinCanUtil.VERB_PASSED))
                courseProgress.setStatus(MessageID.failed_message);
            else if(stmtVerb.equals(UMTinCanUtil.VERB_PASSED))
                courseProgress.setStatus(MessageID.passed);

            courseProgress.setProgress(stmt.getResultProgress());
            courseProgress.setScore(Math.round(stmt.getResultScoreScaled() * 100));

            return courseProgress;
        }
    }

    @Override
    public int registerUser(String username, String password, Hashtable fields, Object context) {
        UserManager userManager =
                PersistenceManager.getInstance().getManager(UserManager.class);
        UserCustomFieldsManager userCustomFieldsManager =
                PersistenceManager.getInstance().getManager(UserCustomFieldsManager.class);

        String loggedInUsername = null;
        loggedInUsername = UstadMobileSystemImpl.getInstance().getActiveUser(context);
        //ignore loggedInUsername cause if we're clicking register, we want this user
        //to log in..

        User loggedInUser = null;
        List<User> users = userManager.findByUsername(context, username);
        if(users!= null && !users.isEmpty()){
            loggedInUser = users.get(0);
        }else{
            //create the user
            try {
                loggedInUser = (User)userManager.makeNew();
                loggedInUser.setUsername(username);
                loggedInUser.setUuid(UUID.randomUUID().toString());
                loggedInUser.setPassword(password);
                loggedInUser.setNotes("User Created via Registration Page");
                loggedInUser.setDateCreated(System.currentTimeMillis());
                userManager.persist(context, loggedInUser);
                userCustomFieldsManager.createUserCustom(fields,loggedInUser, context);
            } catch (SQLException e) {
                e.printStackTrace();
                return 1;
            }
        }

        handleUserLoginAuthComplete(loggedInUser.getUsername(), loggedInUser.getPassword(), context);
        return 0;
    }

    /**
     * Utility merge of what happens after a user is logged in through username/password
     * and what happens after they are newly registered etc.
     */
    private void handleUserLoginAuthComplete(final String username, final String password, Object dbContext) {
        setActiveUser(username, dbContext);
        setActiveUserAuth(password, dbContext);
        String authHashed = hashAuth(dbContext, password);
        setAppPref("um-authcache-" + username, authHashed, dbContext);

    }

    @Override
    public boolean handleLoginLocally(String username, String password, Object dbContext) {
        UserManager userManager = PersistenceManager.getInstance().getManager(UserManager.class);
        boolean result = userManager.authenticate(dbContext, username, password);
        if(result){
            handleUserLoginAuthComplete(username, password, dbContext);
        }
        return result;

    }

    @Override
    public boolean createUserLocally(String username, String password, String uuid, Object dbContext) {
        UserManager userManager = PersistenceManager.getInstance().getManager(UserManager.class);
        try {
            User user = (User) userManager.makeNew();
            if(uuid != null && !uuid.isEmpty()){
                user.setUuid(uuid);
            }else {
                /*
                 * TODO: Change me: The absence of this was causing null pointer exception
                 */
                user.setUuid(UMUUID.randomUUID().toString());
            }
            user.setUsername(username);
            user.setPassword(password);
            userManager.persist(dbContext, user);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
