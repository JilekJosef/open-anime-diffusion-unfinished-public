let mode = new URL(window.location.href).searchParams.get("mode");
if(mode == null){
    mode = "global";
}
let modelArray;
window.addEventListener("DOMContentLoaded", (event) => {
    document.getElementById("modelList").innerHTML = "";
    renderModels();
});

function renderModels(){
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/getmodels", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            location.reload();
        }else{
            modelArray = JSON.parse(xmlHttp.response);
            modelsToHTML(modelArray);
        }
    }
    if(mode !== "global"){
        xmlHttp.send(mode+"\n"+localStorage.getItem('user'));
    }else{
        xmlHttp.send(mode);
    }
}

function modelsToHTML(){
    let filteredArray;
    if(document.getElementById("searchModels").value === ""){
        filteredArray = modelArray;
    }else{
        filteredArray = filterAndSort(modelArray);
    }
    let out = "<div class=\"reflow-product-list ref-cards\"><div class=\"ref-products\">";
    if(filteredArray.length){
        for (let i = 0; i < filteredArray.length; i++) {
            out +=
                "<a class=\"ref-product\" href=\"/modelpage.html?modelid=" + filteredArray[i].id + "\">" +
                "    <div class=\"ref-media\"><img class=\"ref-image\" src=\"" + filteredArray[i].thumbnail + "\" loading=\"lazy\" />" +
                "        <div class=\"ref-sale-badge\">" + filteredArray[i].type + "</div>" +
                "    </div>" +
                "        <div class=\"ref-product-data\">" +
                "            <div class=\"ref-product-info\">" +
                "                <h5 class=\"ref-name\">" + filteredArray[i].name + "</h5>" +
                "                <p class=\"ref-excerpt\">" + filteredArray[i].rating + " " + filteredArray[i].tags + "</p>" +
                "            </div>" +
                "            <strong class=\"ref-price ref-on-sale\"><span style='color: var(--bs-green);'>▲" + filteredArray[i].liked + "</span> <span style='color: var(--bs-red);'>" + filteredArray[i].disliked + "▼</span></strong>" +
                "        </div>" +
                "</a>"
        }
    }
    out += "</div></div>";
    document.getElementById("modelList").innerHTML = out;
}

function filterAndSort(modelList){
    const options = {
        isCaseSensitive: false,
        // includeScore: false,
        // shouldSort: true,
        // includeMatches: false,
        // findAllMatches: false,
        // minMatchCharLength: 1,
        // location: 0,
        // threshold: 0.6,
        // distance: 100,
        useExtendedSearch: true,
        // ignoreLocation: false,
        // ignoreFieldNorm: false,
        // fieldNormWeight: 1,
        keys: [
            "title",
            "rating",
            "type",
            "tags"
        ]
    };

    const fuse = new Fuse(modelList, options);

// Change the pattern
    const pattern = document.getElementById("searchModels").value;

    const result = fuse.search(pattern);
    let out;
    if(result.length){
        out = new Array(result.length);
        for (let i = 0; i < result.length; i++) {
            out[i] = result[i].item;
        }
    }else{
        out = new Array(0);
    }


    return out;
}

function search(){
    modelsToHTML();
}