const modelID = new URL(window.location.href).searchParams.get("modelid");
window.addEventListener("DOMContentLoaded", (event) => {

    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/getmodel", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            location.reload();
        }else{
            initState(JSON.parse(xmlHttp.response));
        }
    }
    xmlHttp.send(modelID+"\n"+localStorage.getItem('user'));
});

function initState(responseObj){
    if(responseObj.upvoted){
        document.getElementById("upvote").setAttribute("style", "background-color: var(--bs-green);");
        document.getElementById("downvote").setAttribute("style", "background-color: var(--bs-gray);");
    }
    if(responseObj.downvoted){
        document.getElementById("downvote").setAttribute("style", "background-color: var(--bs-red);");
        document.getElementById("upvote").setAttribute("style", "background-color: var(--bs-gray);");
    }
    if(responseObj.favourited){
        document.getElementById("addToLiked").setAttribute("style", "background-color: var(--bs-green);");
    }
    if(responseObj.owned){
        document.getElementById("editButtons").setAttribute("style", " ");
    }
    document.getElementById("modelName").innerHTML = responseObj.name;
    document.getElementById("modelDescription").innerHTML = responseObj.descriptionHTML;
}

function upvote(){
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/upvotemodel", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            //location.reload();
        }else{
            if(xmlHttp.responseText === "login"){
                alert("Not logged in");
            }else{
                document.getElementById("upvote").setAttribute("style", "background-color: var(--bs-green);");
                document.getElementById("downvote").setAttribute("style", "background-color: var(--bs-gray);");
            }
        }
    }
    xmlHttp.send(modelID+"\n"+localStorage.getItem('user')+"\n"+localStorage.getItem('token'));
}
function downvote(){
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/downvotemodel", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            //location.reload();
        }else{
            if(xmlHttp.responseText === "login"){
                alert("Not logged in");
            }else{
                document.getElementById("downvote").setAttribute("style", "background-color: var(--bs-red);");
                document.getElementById("upvote").setAttribute("style", "background-color: var(--bs-gray);");
            }
        }
    }
    xmlHttp.send(modelID+"\n"+localStorage.getItem('user')+"\n"+localStorage.getItem('token'));
}
function addToLiked(){
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/addtolikedmodel", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            //location.reload();
        }else{
            if(document.getElementById("addToLiked").getAttribute("style") === "background-color: var(--bs-green);"){
                document.getElementById("addToLiked").setAttribute("style", "");
            }else{
                document.getElementById("addToLiked").setAttribute("style", "background-color: var(--bs-green);");
            }
        }
    }
    xmlHttp.send(modelID+"\n"+localStorage.getItem('user')+"\n"+localStorage.getItem('token'));
}

function edit(){
    window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/addmodel.html?edit="+modelID)
}

function deleteModel(){
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/deletemodel", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            //location.reload();
        }else{
            if(xmlHttp.responseText === "success"){
                alert("Successfully deleted");
                window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/models.html?mode=my")
            }
        }
    }
    xmlHttp.send(modelID+"\n"+localStorage.getItem('user')+"\n"+localStorage.getItem('token'));
}