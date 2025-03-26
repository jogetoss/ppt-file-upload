<div class="form-cell" ${elementMetaData!}>
    <#if !(request.getAttribute("org.joget.apps.form.lib.FileUpload")?? || request.getAttribute("org.joget.plugin.enterprise.ImageUpload")??)>
        <link rel="stylesheet" href="${request.contextPath}/js/dropzone/dropzone.css" />
        <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.marketplace.PPTFileUpload/css/pptViewer.css">
        <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.marketplace.PPTFileUpload/css/all.min.css">
        <script type="text/javascript" src="${request.contextPath}/js/dropzone/dropzone.js"></script>
        <script src="${request.contextPath}/plugin/org.joget.apps.form.lib.FileUpload/js/jquery.fileupload.js"></script>
        <script type="text/javascript">
            Dropzone.autoDiscover = false;
        </script>
        <script src="${request.contextPath}/plugin/org.joget.marketplace.PPTFileUpload/pdf.min.js"></script>
        <script>
            pdfjsLib.GlobalWorkerOptions.workerSrc = '${request.contextPath}/plugin/org.joget.marketplace.PPTFileUpload/pdf.worker.min.js';
        </script>
    </#if>
    <label class="label" field-tooltip="${elementParamName!}"> ${element.properties.label} <span class="form-cell-validator">${decoration}</span>
        <#if error??> <span class="form-error-message">${error}</span></#if>
    </label>
    <#assign showUploadIcon=false>
        <#assign firstPptPath="">
            <#if filePaths??>
                <#list filePaths?keys as key>
                    <#assign firstPptPath=key>
                        <#if filePaths[key]?lower_case?ends_with(".ppt") || filePaths[key]?lower_case?ends_with(".pptx")>
                            <#assign showUploadIcon=true>
                        </#if>
                </#list>
            </#if>

<div class="${showUploadIcon?string(" upload-with-icon", "" )}">
    <#if showUploadIcon>
        <div class="upload-icon convert-to-pdf-btn" data-fullpath="${firstPptPath!?html}">
            <#assign prefix=(slideShowIcon?starts_with("fa-slideshare"))?then("fab", "fas" )>
                <i class="${prefix} ${slideShowIcon!}" style="color: ${viewIconColor!};"></i>
        </div>
    </#if>
    <div id="form-fileupload_${elementParamName!}_${element.properties.elementUniqueKey!}" tabindex="0" class="form-fileupload <#if error??>form-error-cell</#if> <#if element.properties.readonly! == 'true'>readonly<#else>dropzone</#if>">
        <#if element.properties.readonly! !='true'>
            <div class="dz-message needsclick">@@form.fileupload.dropFile@@</div>
            <input style="display:none" id="${elementParamName!}" name="${elementParamName!}" type="file" size="${element.properties.size!}" <#if error??>class="form-error-cell"
        </#if>
        <#if element.properties.multiple! == 'true'>multiple</#if> /> </#if>

            <ul class="form-fileupload-value">
            <#if element.properties.readonly! !='true'>
                <li class="template" style="display:none;">
                    <span class="name" data-dz-name></span>
                    <a class="remove" style="display:none">@@form.fileupload.remove@@</a>
                    <strong class="error text-danger" data-dz-errormessage></strong>
                    <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
                        <div class="progress-bar progress-bar-success" style="width:0%;" data-dz-uploadprogress></div>
                    </div>
                    <input type="hidden" name="${elementParamName!}_path" value="" disabled />
                </li>
            </#if>

                <#if tempFilePaths??>
                    <#list tempFilePaths?keys as key>
                        <li>
                            <span class="name">${tempFilePaths[key]!}</span>
                            <#if element.properties.readonly! !='true'>
                                <a class="remove">@@form.fileupload.remove@@</a>
                            </#if>
                            <input type="hidden" name="${elementParamName!}_path" value="${key!?html}" />
                            <#if tempFilePaths[key]?lower_case?ends_with(".ppt") || tempFilePaths[key]?lower_case?ends_with(".pptx")>
                                <button type="button" class="convert-to-pdf-btn" data-fullpath="${key!?html}" style="background-color: ${viewPPTButtonColor!};"> ${viewPPTButtonLabel!} </button>
                            </#if>
                        </li>
                    </#list>
                </#if>

                <#if filePaths??>
                    <#list filePaths?keys as key>
                        <li>
                            <a href="${request.contextPath}${key!?html}" target="_blank">
                                <span class="name">${filePaths[key]!}</span>
                            </a>
                            <#if element.properties.readonly! != 'true'>
                                <a class="remove">@@form.fileupload.remove@@</a>
                            </#if>
                            <input type="hidden" name="${elementParamName!}_path" value="${filePaths[key]!}" />
                            <#if filePaths[key]?lower_case?ends_with(".ppt") || filePaths[key]?lower_case?ends_with(".pptx")>
                                
                            </#if>
                        </li>
                    </#list>
                </#if>
            </ul>
        </div>
    </div> 


<div id="pdfViewerModal" class="pdf-modal">
    <div class="pdf-modal-content">
        <span class="pdf-close">&times;</span>

        <div id="pdfViewerContainer">
            <div id="pdfToolbar" class="pdf-toolbar">
                <div class="pdf-nav-controls">
                    <span id="pageNum" class="pdf-page-info">Page: <span id="currentPage">0</span> / <span id="totalPages">0</span></span>
                </div>
                <button id="fullscreenToggle" class="pdf-fullscreen-btn" title="Toggle Fullscreen">
                    <i class="fa fa-expand"></i>
                </button>
            </div>
            <div id="pdfCanvasContainer" class="pdf-canvas-container">
                <button id="prevArrow" class="pdf-nav-arrow pdf-nav-arrow-left">&lsaquo;</button>
                <canvas id="pdfCanvas" style="object-fit: !contain;"></canvas>
                <button id="nextArrow" class="pdf-nav-arrow pdf-nav-arrow-right">&rsaquo;</button>
            </div>
        </div>

<div id="pdfLoadingOverlay" class="pdf-loading-overlay">
    <div class="spinner-container">
        <div class="spinner"></div>
        <div class="loading-text">Loading PowerPoint slides...</div>
    </div>
</div>
</div>
</div>
    
   
    <script>
    $(document).ready(function(){
        // Standard file upload initialization
        $('#form-fileupload_${elementParamName!}_${element.properties.elementUniqueKey!}').fileUploadField({
            url : "${element.serviceUrl!}",
            paramName : "${elementParamName!}",
            multiple : "${element.properties.multiple!}",
            maxSize : "${element.properties.maxSize!}",
            maxSizeMsg : "${element.properties.maxSizeMsg!}",
            fileType : "${element.properties.fileType!}",
            fileTypeMsg : "${element.properties.fileTypeMsg!}",
            padding : "${element.properties.padding!}",
            removeFile : "${element.properties.removeFile!}",
            resizeWidth : "${element.properties.resizeWidth!}",
            resizeHeight : "${element.properties.resizeHeight!}",
            resizeQuality : "${element.properties.resizeQuality!}",
            resizeMethod : "${element.properties.resizeMethod!}"
        });

const $modal = $("#pdfViewerModal");
const $closeButton = $modal.find(".pdf-close");
const $loadingOverlay = $("#pdfLoadingOverlay");

const $modalContent = $(".pdf-modal-content");
const $toolbar = $(".pdf-toolbar");

$modalContent.css("position", "absolute"); 

$toolbar.on("mousedown", function (e) {
    let isDragging = true;

    const startX = e.clientX;
    const startY = e.clientY;

    const startLeft = $modalContent.offset().left;
    const startTop = $modalContent.offset().top;

    $(document).on("mousemove.modalDrag", function (e) {
        if (!isDragging) return;

        const newLeft = startLeft + (e.clientX - startX);
        const newTop = startTop + (e.clientY - startY);

        $modalContent.offset({
            left: newLeft,
            top: newTop,
        });
    });

    $(document).on("mouseup.modalDrag", function () {
        isDragging = false;
        $(document).off("mousemove.modalDrag mouseup.modalDrag");
    });

    e.preventDefault();
});

let pdfDoc = null;
let pageNum = 1;
let pageRendering = false;
let pageNumPending = null;
let scale = 1;
const canvas = document.getElementById("pdfCanvas");
const ctx = canvas.getContext("2d");

function checkPdfJsLoaded() {
    if (typeof pdfjsLib !== "undefined") {
        pdfjsLib.GlobalWorkerOptions.workerSrc =
            "${request.contextPath}/plugin/org.joget.marketplace.PPTFileUpload/pdf.worker.min.js";
        return true;
    }
    return false;
}


if (!checkPdfJsLoaded()) {
    var script = document.createElement('script');
    script.src = '${request.contextPath}/plugin/org.joget.marketplace.PPTFileUpload/pdf.min.js';
    script.onload = function() {
  pdfjsLib.GlobalWorkerOptions.workerSrc = '${request.contextPath}/plugin/org.joget.marketplace.PPTFileUpload/pdf.worker.min.js';
    };
    document.head.appendChild(script);
}

        $(".convert-to-pdf-btn").on("click", function() {
            var $btn = $(this);
            var fullPath = $btn.data("fullpath");
            
            

            if (fullPath.includes('?')) {
                fullPath = fullPath.split('?')[0];
            }
            if (fullPath.startsWith("/")) {
                fullPath = fullPath.substring(1);
            }
            

            var serviceUrl = "${element.serviceUrl!}";
            var separator = serviceUrl.includes('?') ? '&' : '?';
            var requestUrl = serviceUrl + separator + "_path=" + encodeURIComponent(fullPath);
            
            var fullRequestUrl = window.location.origin + 
                (requestUrl.startsWith("/") ? requestUrl : "/" + requestUrl);
            
            
            $btn.prop("disabled", true);
            
            $modal.css("display", "block");
            $loadingOverlay.show();
            
            if (pdfDoc) {
                pdfDoc.destroy();
                pdfDoc = null;
            }
            pageNum = 1;
            
            loadPdf(fullRequestUrl).then(() => {
                $loadingOverlay.fadeOut(300);
                $btn.prop("disabled", false);
            }).catch(error => {
                $loadingOverlay.fadeOut(300);
                $btn.prop("disabled", false);
                alert("Failed to load PDF. Please try again.");
            });
            
            setTimeout(function() {
                if($btn.prop("disabled")) {
                    $btn.prop("disabled", false).text("${viewPPTButtonLabel!}");
                }
                if($loadingOverlay.is(":visible")) {
                    $loadingOverlay.fadeOut(300);
                }
            }, 15000);
        });
        
        async function loadPdf(url) {
            try {
                const loadingTask = pdfjsLib.getDocument(url);
                pdfDoc = await loadingTask.promise;
                
                document.getElementById('totalPages').textContent = pdfDoc.numPages;
                
                renderPage(pageNum);
                
                   document.getElementById('prevArrow').addEventListener('click', function(e) {
                   e.preventDefault();
              e.stopPropagation();
               onPrevPage();
                });
                
             document.getElementById('nextArrow').addEventListener('click', function(e) {
    e.preventDefault();
    e.stopPropagation();
    onNextPage();
});
          
             if (document.getElementById('scaleSelect')?.value === 'auto') {
    fitToContainer();
}
                
            } catch (error) {
                console.error("Error loading PDF:", error);
                throw error;
            }
        }
        
function fitToContainer() {
    if (!pdfDoc) return;
    
    pdfDoc.getPage(pageNum).then(function(page) {
        const container = document.getElementById('pdfCanvasContainer');
        const canvas = document.getElementById('pdfCanvas');

        const containerWidth = container.clientWidth;
        const containerHeight = container.clientHeight;

        const viewport = page.getViewport({ scale: 1 });

        const widthScale = containerWidth / viewport.width;
        const heightScale = containerHeight / viewport.height;

        scale = Math.min(widthScale, heightScale);

        canvas.width = viewport.width * scale;
        canvas.height = viewport.height * scale;

        queueRenderPage(pageNum);
    });
}



        
 function renderPage(num, direction) {
    pageRendering = true;
    
    const canvas = document.getElementById('pdfCanvas');
    if (direction === 'next') {
        canvas.classList.add('slide-left-transition');
    } else if (direction === 'prev') {
        canvas.classList.add('slide-right-transition');
    } else {
        canvas.classList.add('fade-transition');
    }
    
    document.getElementById('currentPage').textContent = num;
if (document.getElementById('currentPageOverlay')) {
        document.getElementById('currentPageOverlay').textContent = num;
    }

    pdfDoc.getPage(num).then(function(page) {
        var viewport = page.getViewport({ scale: scale });

        canvas.height = viewport.height;
        canvas.width = viewport.width;

        var renderContext = {
            canvasContext: ctx,
            viewport: viewport
        };

        var renderTask = page.render(renderContext);

        renderTask.promise.then(function() {
            setTimeout(() => {
                canvas.classList.remove('fade-transition');
                canvas.classList.remove('slide-left-transition');
                canvas.classList.remove('slide-right-transition');
            }, 800); // Match the animation duration
            
            pageRendering = false;
            
            if (pageNumPending !== null) {
                const pendingDirection = pageNumPending > num ? 'next' : 'prev';
                renderPage(pageNumPending, pendingDirection);
                pageNumPending = null;
            }
        });
    });
}

        
     function queueRenderPage(num, direction) {
    if (pageRendering) {
        pageNumPending = num;
    } else {
        renderPage(num, direction);
    }
}
        
function onPrevPage() {
    if (pageNum <= 1) {
        return;
    }
    pageNum--;
    queueRenderPage(pageNum, 'prev');
}
        
function onNextPage() {
    if (pageNum >= pdfDoc.numPages) {
        return;
    }
    pageNum++;
    queueRenderPage(pageNum, 'next');
}
    
        
$closeButton.on("click", function() {
    $modalContent.css({
        "top": "50%",
        "left": "50%",
        "transform": "translate(-50%, -50%)",
        "width": "70%",
        "height": "85vh",
        "max-width": "1000px",
        "max-height": "90vh"
    });
    
            $modal.css("display", "none");
            
            if (pdfDoc) {
                pdfDoc.destroy();
                pdfDoc = null;
            }
});
        
    
        
        $(document).on("keydown", function(e) {
            if (!pdfDoc || !$modal.is(":visible")) return;
            
            if (e.key === "Escape") {
                $closeButton.trigger("click");
            } else if (e.key === "ArrowRight") {
                e.preventDefault(); 
                onNextPage();
            } else if (e.key === "ArrowLeft") {
                e.preventDefault(); 
                onPrevPage();
            } 
        });
        
        $(window).on("resize", function() {
    if (pdfDoc) {
        fitToContainer();
    }
});

$("#fullscreenToggle").on("click", function(e) {
    e.preventDefault();
    e.stopPropagation();
    
    const modalContent = document.querySelector('.pdf-modal-content');
    const $icon = $(this).find('i');
    
    if (!document.fullscreenElement) {
        if (modalContent.requestFullscreen) {
            modalContent.requestFullscreen();
        } else if (modalContent.webkitRequestFullscreen) {
            modalContent.webkitRequestFullscreen();
        } else if (modalContent.msRequestFullscreen) {
            modalContent.msRequestFullscreen();
        }
        $icon.removeClass('fa-expand').addClass('fa-compress');
        
        $('#currentPageOverlay').text($('#currentPage').text());
        $('#totalPagesOverlay').text($('#totalPages').text());
    } else {

        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.webkitExitFullscreen) {
            document.webkitExitFullscreen();
        } else if (document.msExitFullscreen) {
            document.msExitFullscreen();
        }
        $icon.removeClass('fa-compress').addClass('fa-expand');
    }
    
    setTimeout(function() {
        if (pdfDoc) {
            fitToContainer();
        }
    }, 300);
});

$(document).on('fullscreenchange webkitfullscreenchange mozfullscreenchange MSFullscreenChange', function() {
    const $icon = $("#fullscreenToggle").find('i');
    if (document.fullscreenElement) {
        $icon.removeClass('fa-expand').addClass('fa-compress');
    } else {
        $icon.removeClass('fa-compress').addClass('fa-expand');
    }
    
    if (pdfDoc) {
        fitToContainer();
    }
});
    });

    </script>
</div>