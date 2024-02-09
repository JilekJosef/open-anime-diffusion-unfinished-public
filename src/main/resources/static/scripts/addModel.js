const edit = new URL(window.location.href).searchParams.get("edit");
window.addEventListener("DOMContentLoaded", (event) => {
    if(edit !== null){
        let xmlHttp = new XMLHttpRequest();
        xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/getmodel", true ); // false for synchronous request
        xmlHttp.onload = () =>{
            if(xmlHttp.status !== 200){
                alert("Connection error");
                location.reload();
            }else{
                let modelObj = JSON.parse(xmlHttp.response);

                document.getElementById("modelName").value = modelObj.name;
                document.getElementById("modelTags").value = modelObj.tags;
                document.getElementById("thubnailLink").value = modelObj.thumbnail;
                document.getElementById("modelType").innerHTML = modelObj.type;
                document.getElementById("modelRating").innerHTML = modelObj.rating;
                document.getElementById("description").value = modelObj.descriptionHTML;

                document.getElementById("addB").innerHTML = "Update";
                document.getElementById("addB").setAttribute("onclick", "updateModel()");
            }
        }
        xmlHttp.send(edit+"\n"+" ");
    }
});
function addModel(){
    const modelName = document.getElementById("modelName").value;
    const modelTags = document.getElementById("modelTags").value;
    const thumbnailLink = document.getElementById("thubnailLink").value;
    const modelType = document.getElementById("modelType").innerHTML;
    const modelRating = document.getElementById("modelRating").innerHTML;
    const description = document.getElementById("description").value;

    const model = {
        modelName: modelName,
        modelTags: modelTags,
        thumbnailLink: thumbnailLink,
        modelType: modelType,
        modelRating: modelRating,
        description: description
    }

    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/addmodel", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            location.reload();
        }else{
            if(xmlHttp.responseText !== "success"){
                alert(xmlHttp.responseText);
            }else{
                window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/models.html?mode=my");
            }
        }
    }
    xmlHttp.send(localStorage.getItem('user')+"\n"+localStorage.getItem('token')+"\n"+JSON.stringify(model));
    return false;
}

function changeType(changeTo){
    document.getElementById("modelType").innerHTML = changeTo;
    return false;
}
function changeRating(changeTo){
    document.getElementById("modelRating").innerHTML = changeTo;
    return false;
}

function updateModel(){
    const modelName = document.getElementById("modelName").value;
    const modelTags = document.getElementById("modelTags").value;
    const thumbnailLink = document.getElementById("thubnailLink").value;
    const modelType = document.getElementById("modelType").innerHTML;
    const modelRating = document.getElementById("modelRating").innerHTML;
    const description = document.getElementById("description").value;

    const model = {
        modelName: modelName,
        modelTags: modelTags,
        thumbnailLink: thumbnailLink,
        modelType: modelType,
        modelRating: modelRating,
        description: description
    }

    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/editmodel", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            location.reload();
        }else{
            if(xmlHttp.responseText !== "success"){
                alert(xmlHttp.responseText);
            }else{
                window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/models.html?mode=my");
            }
        }
    }
    xmlHttp.send(localStorage.getItem('user')+"\n"+localStorage.getItem('token')+"\n"+edit+"\n"+JSON.stringify(model));
    return false;
}