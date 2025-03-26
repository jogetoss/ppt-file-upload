package org.joget.marketplace;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FileDownloadSecurity;
import org.joget.apps.form.model.Form;
import org.joget.commons.util.SecurityUtil;
import org.json.JSONObject;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormPermission;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.Permission;
import org.joget.apps.userview.model.PwaOfflineResources;
import org.joget.commons.util.FileManager;
import org.joget.commons.util.FileStore;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.directory.model.User;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import java.io.*;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.joget.apps.app.service.AppService;
import org.joget.apps.form.service.FileUtil;

public class PPTFileUpload extends Element implements FormBuilderPaletteElement, FileDownloadSecurity, PluginWebSupport, PwaOfflineResources {

    private final static String MESSAGE_PATH = "messages/pptFileUpload";

    @Override
    public String getName() {
        return "PPT File Upload";
    }

    @Override
    public String getVersion() {
        return "8.0.0";
    }

    @Override
    public String getDescription() {

        return "PPT FileUpload Element";

    }

    @Override
    public String getLabel() {
        return "PPT File Upload";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/pptFileUpload.json", null, true, MESSAGE_PATH);
    }

    @Override
    public String getFormBuilderCategory() {
        return "Marketplace";
    }

    @Override
    public int getFormBuilderPosition() {
        return 300;

    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fas fa-upload\"></i>";
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>" + ResourceBundleUtil.getMessage("org.joget.apps.form.lib.FileUpload.pluginLabel") + "</label><input type='file' />";
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "pptFileUpload.ftl";
        String viewIconColor = (String) getProperty("viewIconColor");
        String slideShowIcon = (String) getProperty("slideShowIcon");

        dataModel.put("viewIconColor", viewIconColor);
        dataModel.put("slideShowIcon", slideShowIcon);
        // set value
        String[] values = FormUtil.getElementPropertyValues(this, formData);

        Map<String, String> tempFilePaths = new LinkedHashMap<String, String>();
        Map<String, String> filePaths = new LinkedHashMap<String, String>();

        String primaryKeyValue = getPrimaryKeyValue(formData);
        String filePathPostfix = "_path";
        String id = FormUtil.getElementParameterName(this);

        //check is there a stored value
        String storedValue = formData.getStoreBinderDataProperty(this);
        if (storedValue != null) {
            values = storedValue.split(";");
        } else {
            //if there is no stored value, get the temp files
            String[] tempExisting = formData.getRequestParameterValues(id + filePathPostfix);

            if (tempExisting != null && tempExisting.length > 0) {
                values = tempExisting;
            }
        }

        String formDefId = "";
        Form form = FormUtil.findRootForm(this);
        if (form != null) {
            formDefId = form.getPropertyString(FormUtil.PROPERTY_ID);
        }
        String appId = "";
        String appVersion = "";

        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        if (appDef != null) {
            appId = appDef.getId();
            appVersion = appDef.getVersion().toString();
        }

        for (String value : values) {
            // check if the file is in temp file
            File file = FileManager.getFileByPath(value);

            if (file != null) {
                tempFilePaths.put(value, file.getName());
            } else if (value != null && !value.isEmpty()) {
                // determine actual path for the file uploads
                String fileName = value;
                String encodedFileName = fileName;
                if (fileName != null) {
                    try {
                        encodedFileName = URLEncoder.encode(fileName, "UTF8").replaceAll("\\+", "%20");
                    } catch (UnsupportedEncodingException ex) {
                        // ignore
                    }
                }

                String filePath = "/web/client/app/" + appId + "/" + appVersion + "/form/download/" + formDefId + "/" + primaryKeyValue + "/" + encodedFileName + ".";
                if (Boolean.valueOf(getPropertyString("attachment")).booleanValue()) {
                    filePath += "?attachment=true";
                }
                filePaths.put(filePath, value);
            }
        }

        if (!tempFilePaths.isEmpty()) {
            dataModel.put("tempFilePaths", tempFilePaths);
        }
        if (!filePaths.isEmpty()) {
            dataModel.put("filePaths", filePaths);
        }

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    @Override
    public Boolean selfValidate(FormData formData) {
        String id = FormUtil.getElementParameterName(this);
        Boolean valid = true;
        String error = "";
        try {
            String[] values = FormUtil.getElementPropertyValues(this, formData);

            for (String value : values) {
                File file = FileManager.getFileByPath(value);
                if (file != null) {
                    if (getPropertyString("maxSize") != null && !getPropertyString("maxSize").isEmpty()) {
                        long maxSize = Long.parseLong(getPropertyString("maxSize")) * 1024;

                        if (file.length() > maxSize) {
                            valid = false;
                            error += getPropertyString("maxSizeMsg") + " ";

                        }
                    }
                    if (getPropertyString("fileType") != null && !getPropertyString("fileType").isEmpty()) {
                        String[] fileType = getPropertyString("fileType").split(";");
                        String filename = file.getName().toUpperCase();
                        Boolean found = false;
                        for (String type : fileType) {
                            if (filename.endsWith(type.toUpperCase())) {
                                found = true;
                            }
                        }
                        if (!found) {
                            valid = false;
                            error += getPropertyString("fileTypeMsg");
                            FileManager.deleteFile(file);
                        }
                    }
                }
            }

            if (!valid) {
                formData.addFormError(id, error);
            }
        } catch (Exception e) {
        }

        return valid;
    }

    public boolean isDownloadAllowed(Map requestParameters) {
        String permissionType = getPropertyString("permissionType");
        if (permissionType.equals("public")) {
            return true;
        } else if (permissionType.equals("custom")) {
            Object permissionElement = getProperty("permissionPlugin");
            if (permissionElement != null && permissionElement instanceof Map) {
                Map elementMap = (Map) permissionElement;
                String className = (String) elementMap.get("className");
                Map<String, Object> properties = (Map<String, Object>) elementMap.get("properties");
    
                //convert it to plugin
                PluginManager pm = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
                Permission plugin = (Permission) pm.getPlugin(className);
                if (plugin != null && plugin instanceof FormPermission) {
                    WorkflowUserManager workflowUserManager = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
                    User user = workflowUserManager.getCurrentUser();

                    plugin.setProperties(properties);
                    plugin.setCurrentUser(user);
                    plugin.setRequestParameters(requestParameters);

                    return plugin.isAuthorize();
                }
            }
            return false;
        } else {
            return !WorkflowUtil.isCurrentUserAnonymous();
        }
    }

    public String getServiceUrl() {
        String url = WorkflowUtil.getHttpServletRequest().getContextPath() + "/web/json/plugin/org.joget.marketplace.PPTFileUpload/service";
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        // Create nonce
        String paramName = FormUtil.getElementParameterName(this);
        String fileType = getPropertyString("fileType"); // Dynamically configured file types
        String nonce = SecurityUtil.generateNonce(new String[]{"FileUpload", appDef.getAppId(), appDef.getVersion().toString(), paramName, fileType}, 1);

        try {
            url = url + "?_nonce=" + URLEncoder.encode(nonce, "UTF-8")
                    + "&_paramName=" + URLEncoder.encode(paramName, "UTF-8")
                    + "&_appId=" + URLEncoder.encode(appDef.getAppId(), "UTF-8")
                    + "&_appVersion=" + URLEncoder.encode(appDef.getVersion().toString(), "UTF-8")
                    + "&_ft=" + URLEncoder.encode(fileType, "UTF-8");
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error generating service URL");
        }

        return url;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String nonce = request.getParameter("_nonce");
        String paramName = request.getParameter("_paramName");
        String appId = request.getParameter("_appId");
        String appVersion = request.getParameter("_appVersion");
        String filePath = request.getParameter("_path");
        String fileType = request.getParameter("_ft");
        

        if (!SecurityUtil.verifyNonce(nonce, new String[]{"FileUpload", appId, appVersion, paramName, fileType})) {
            LogUtil.warn(getClass().getName(), "Nonce verification failed - security check");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ResourceBundleUtil.getMessage("general.error.error403"));
            return;
        }

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            handleFileUpload(response, paramName);
        } else if (filePath != null && !filePath.isEmpty()) {
            handleFileRetrieval(request, response, filePath);
        }
    }

   
    private void handleFileUpload(HttpServletResponse response, String paramName) throws IOException {

        try {
            JSONObject obj = new JSONObject();
            try {
                // handle multipart files
                String validatedParamName = SecurityUtil.validateStringInput(paramName);
                MultipartFile file = FileStore.getFile(validatedParamName);
                if (file != null && file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
                    String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();

                    String path = FileManager.storeFile(file);
                    obj.put("path", path);
                    obj.put("filename", file.getOriginalFilename());
                    obj.put("newFilename", path.substring(path.lastIndexOf(File.separator) + 1));

                }

                Collection<String> errorList = FileStore.getFileErrorList();
                if (errorList != null && !errorList.isEmpty() && errorList.contains(paramName)) {
                    obj.put("error", ResourceBundleUtil.getMessage("general.error.fileSizeTooLarge", new Object[]{FileStore.getFileSizeLimit()}));
                }
            } catch (Exception e) {
                obj.put("error", e.getLocalizedMessage());
            } finally {
                FileStore.clear();
            }
            obj.write(response.getWriter());
        } catch (Exception ex) {
        }
    }

    private void handleFileRetrieval(HttpServletRequest request, HttpServletResponse response, String filePath) throws IOException {

        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

        String appId = request.getParameter("_appId");  
        if (appId == null || appId.isEmpty()) {
            LogUtil.error(getClass().getName(), null, "App ID is missing in request.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "App ID is required.");
            return;
        }

        AppDefinition appDef = appService.getPublishedAppDefinition(appId);
        if (appDef == null) {
            LogUtil.error(getClass().getName(), null, "AppDefinition is null for appId: " + appId);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Application definition not found.");
            return;
        }

        String[] pathParts = filePath.split("/");
        String formId = null;
        String primaryKey = null;
        String fileName = null;

        for (int i = 0; i < pathParts.length; i++) {
            if ("download".equals(pathParts[i]) && i + 3 < pathParts.length) {
                formId = pathParts[i + 1];
                primaryKey = pathParts[i + 2];
                fileName = pathParts[i + 3];

                if (fileName.contains(".?")) {
                    fileName = fileName.substring(0, fileName.indexOf(".?"));
                } else if (fileName.endsWith(".")) {
                    fileName = fileName.substring(0, fileName.length() - 1);
                }
                fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
                break;
            }
        }

        if (formId == null || primaryKey == null || fileName == null) {
            LogUtil.warn(getClass().getName(), "Invalid file path format: " + filePath);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path format");
            return;
        }

        String tableName = appService.getFormTableName(appDef, formId);
        if (tableName == null || tableName.isEmpty()) {
            LogUtil.error(getClass().getName(), null, "Failed to retrieve table name for formDefId: " + formId);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not determine table name.");
            return;
        }


        File file = null;
        try {
            file = FileUtil.getFile(fileName, tableName, primaryKey);
            if (file != null) {
            }
        } catch (IOException e) {
            LogUtil.error(getClass().getName(), e, "Error retrieving file");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving file");
            return;
        }

        if (file == null || !file.exists()) {
            LogUtil.warn(getClass().getName(), "File not found: " + fileName);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        try {
            convertPptxToPdf(file, response);
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error converting and streaming PDF file");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error converting file to PDF");
        }
    }

    private void convertPptxToPdf(File pptxFile, HttpServletResponse response) {
        File pdfFile = null;

        try {
            PDDocument doc = new PDDocument();
            Dimension pgsize;
            int slideCount;

            // Determine file type by extension
            String fileName = pptxFile.getName().toLowerCase();

            if (fileName.endsWith(".pptx")) {
                // Handle PPTX files
                try (FileInputStream fis = new FileInputStream(pptxFile); XMLSlideShow ppt = new XMLSlideShow(fis)) {

                    pgsize = ppt.getPageSize();
                    List<XSLFSlide> slides = ppt.getSlides();

                    int slideIndex = 1;
                    for (XSLFSlide slide : slides) {

                        // Create a high-quality image of the slide
                        BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = img.createGraphics();

                        // Apply rendering hints for better quality
                        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                        // Draw white background and the slide content
                        graphics.setPaint(Color.white);
                        graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));
                        slide.draw(graphics);
                        graphics.dispose();

                        // Add the slide image to the PDF
                        PDPage page = new PDPage(new PDRectangle(pgsize.width, pgsize.height));
                        doc.addPage(page);

                        PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img);
                        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
                        contentStream.drawImage(pdImage, 0, 0, pgsize.width, pgsize.height);
                        contentStream.close();

                        slideIndex++;
                    }
                }
            } else if (fileName.endsWith(".ppt")) {
                // Handle PPT files
                try (FileInputStream fis = new FileInputStream(pptxFile); HSLFSlideShow ppt = new HSLFSlideShow(fis)) {

                    pgsize = ppt.getPageSize();
                    List<HSLFSlide> slides = ppt.getSlides();

                    int slideIndex = 1;
                    for (HSLFSlide slide : slides) {

                        // Create a high-quality image of the slide
                        BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = img.createGraphics();

                        // Apply rendering hints for better quality
                        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                        // Draw white background and the slide content
                        graphics.setPaint(Color.white);
                        graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));
                        slide.draw(graphics);
                        graphics.dispose();

                        // Add the slide image to the PDF
                        PDPage page = new PDPage(new PDRectangle(pgsize.width, pgsize.height));
                        doc.addPage(page);

                        PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img);
                        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
                        contentStream.drawImage(pdImage, 0, 0, pgsize.width, pgsize.height);
                        contentStream.close();

                        slideIndex++;
                    }
                }
            } else {
                // Unsupported file format
                LogUtil.error(getClass().getName(), null, "Unsupported file format: " + fileName);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file format");
                return;
            }

            doc.save(response.getOutputStream());
            doc.close();

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error converting PowerPoint to PDF");
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error converting PowerPoint to PDF");
            } catch (IOException ex) {
                LogUtil.error(getClass().getName(), ex, "Error sending error response");
            }
        }
    }

    private void streamFileToResponse(File file, HttpServletRequest request, HttpServletResponse response, String contentType) throws IOException {
        if (file == null || !file.exists()) {
            LogUtil.warn(getClass().getName(), "Attempted to stream a non-existent file: " + (file != null ? file.getAbsolutePath() : "null"));
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
            return;
        }


        // Set response headers
        response.setContentType(contentType != null ? contentType : "application/octet-stream");
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
        response.setHeader("Content-Length", String.valueOf(file.length()));

        // Read and stream the file
        try (InputStream inputStream = new FileInputStream(file); OutputStream outputStream = response.getOutputStream()) {

            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            long totalBytesSent = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesSent += bytesRead;
            }


        } catch (IOException e) {
            LogUtil.error(getClass().getName(), e, "Error occurred while streaming file: " + file.getAbsolutePath());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error streaming file.");
        }
    }

    @Override
    public FormData formatDataForValidation(FormData formData) {
        String filePathPostfix = "_path";
        String id = FormUtil.getElementParameterName(this);
        if (id != null) {
            String[] tempFilenames = formData.getRequestParameterValues(id); //this is for fallback fileupload
            String[] tempDropzone = formData.getRequestParameterValues(id + filePathPostfix); //this is for dropzone upload

            List<String> filenames = new ArrayList<String>();
            if (tempFilenames != null && tempFilenames.length > 0) {
                filenames.addAll(Arrays.asList(tempFilenames));
            }

            if (tempDropzone != null && tempDropzone.length > 0) {
                for (String tempPath : tempDropzone) {
                    //validate to check the temp file is exist before add it
                    if (tempPath.contains(File.separator)) {
                        File file = FileManager.getFileByPath(tempPath);
                        if (file != null && file.exists()) {
                            filenames.add(tempPath);
                        }
                    } else {
                        filenames.add(tempPath);
                    }
                }
            }

            if (filenames.isEmpty()) {
                formData.addRequestParameterValues(id, new String[]{""});
            } else if (!"true".equals(getPropertyString("multiple"))) {
                formData.addRequestParameterValues(id, new String[]{filenames.get(0)});
            } else {
                formData.addRequestParameterValues(id, filenames.toArray(new String[]{}));
            }
        }
        return formData;
    }

    @Override
    public FormRowSet formatData(FormData formData) {
        FormRowSet rowSet = null;

        String id = getPropertyString(FormUtil.PROPERTY_ID);

        Set<String> remove = null;
        if ("true".equals(getPropertyString("removeFile"))) {
            remove = new HashSet<String>();
            Form form = FormUtil.findRootForm(this);
            String originalValues = formData.getLoadBinderDataProperty(form, id);
            if (originalValues != null) {
                remove.addAll(Arrays.asList(originalValues.split(";")));
            }
        }

        // get value
        if (id != null) {
            String[] values = FormUtil.getElementPropertyValues(this, formData);
            if (values != null && values.length > 0) {
                // set value into Properties and FormRowSet object
                FormRow result = new FormRow();
                List<String> resultedValue = new ArrayList<String>();
                List<String> filePaths = new ArrayList<String>();

                for (String value : values) {
                    // check if the file is in temp file
                    File file = FileManager.getFileByPath(value);
                    if (file != null) {
                        filePaths.add(value);
                        resultedValue.add(file.getName());
                    } else {
                        if (remove != null && !value.isEmpty()) {
                            remove.remove(value);
                        }
                        resultedValue.add(value);
                    }
                }

                if (!filePaths.isEmpty()) {
                    result.putTempFilePath(id, filePaths.toArray(new String[]{}));
                }

                if (remove != null) {
                    result.putDeleteFilePath(id, remove.toArray(new String[]{}));
                }

                // formulate values
                String delimitedValue = FormUtil.generateElementPropertyValues(resultedValue.toArray(new String[]{}));
                String paramName = FormUtil.getElementParameterName(this);
                formData.addRequestParameterValues(paramName, resultedValue.toArray(new String[]{}));

                // set value into Properties and FormRowSet object
                result.setProperty(id, delimitedValue);
                rowSet = new FormRowSet();
                rowSet.add(result);

                String filePathPostfix = "_path";
                formData.addRequestParameterValues(id + filePathPostfix, new String[]{});
            }
        }

        return rowSet;
    }

    @Override
    public Set<String> getOfflineStaticResources() {
        Set<String> urls = new HashSet<String>();
        String contextPath = AppUtil.getRequestContextPath();
        urls.add(contextPath + "/js/dropzone/dropzone.css");
        urls.add(contextPath + "/js/dropzone/dropzone.js");
        urls.add(contextPath + "/plugin/org.joget.apps.form.lib.FileUpload/js/jquery.fileupload.js");

        return urls;
    }

}
