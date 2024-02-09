window.addEventListener("DOMContentLoaded", (event) => {
    loginUser();
});
function loginFun(){
    const email = document.getElementById("email").value.toLowerCase();
    const password = document.getElementById("password").value;

    const hash = CryptoJS.SHA256(email+password);

    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/login" , true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            location.reload();
        }else{
            if(xmlHttp.responseText === "email"){
                alert("This account with this email does not exist.")
                document.getElementById("email").focus();
            }else if (xmlHttp.responseText === "pass"){
                alert("Password incorrect.")
                document.getElementById("password").focus();
            }else{
                localStorage.setItem('user', email)
                localStorage.setItem('token', xmlHttp.responseText)
                window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/models.html")
            }
        }
    }
    xmlHttp.send(email+"\n"+hash);
    return false;
}
function loginUser() {
    const token = localStorage.getItem('token')
    if (!token){
        return false;
    }
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/islogged" , true); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            //alert("Connection error");
            //location.reload();
        }else{
            if (xmlHttp.responseText === "true"){
                document.getElementById("loginButton").innerHTML = "Log out";
                document.getElementById("loginButton").setAttribute( "onclick", "logout()");
            }
        }
    }
    xmlHttp.send(localStorage.getItem('user')+"\n"+token);
    return false;
}
function logout () {
    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/logout" , true); // false for synchronous request
    xmlHttp.send(localStorage.getItem('user')+"\n"+localStorage.getItem('token'));
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/login.html");
    return false;
}

function resetPassword(){
    document.getElementById("password").setAttribute("display","none");
    document.getElementById("forgotYourPasswordQ").setAttribute("display","none");
    document.getElementById("deleteAccountQ").setAttribute("display","none");
    document.getElementById("login").innerHTML = "<strong>Send reset email</strong>";
    document.getElementById("login").setAttribute("onclick", "sendResetEmail()");
}

function deleteAccount(){
    document.getElementById("forgotYourPasswordQ").setAttribute("display","none");
    document.getElementById("deleteAccountQ").setAttribute("display","none");
    document.getElementById("dontHaveAnAccountQ").setAttribute("display","none");
    document.getElementById("login").innerHTML = "<strong>DELETE ACCOUNT</strong>";
    document.getElementById("login").setAttribute("onclick", "sendDeleteAccount()");
    document.getElementById("login").setAttribute("background-color", "var(--bs-red)");
}

function sendResetEmail(){
    const email = document.getElementById("email").value.toLowerCase();

    xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/sendresetemail" , true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            //location.reload();
        }else{
            if(xmlHttp.responseText === "true"){
                alert("Email sent. Check your mailbox.")
                window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/login.html")
            }else{
                alert("This email is not registered.")
            }
        }
    }
    xmlHttp.send(email);
    return false;
}

function sendDeleteAccount(){
    const email = document.getElementById("email").value.toLowerCase();
    const password = document.getElementById("password").value;

    const hash = CryptoJS.SHA256(email+password);

    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/deleteaccount", true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            location.reload();
        }else{
            if(xmlHttp.responseText === "email"){
                alert("The account with this email does not exist.")
                document.getElementById("email").focus();
            }else if (xmlHttp.responseText === "pass"){
                alert("Password incorrect.")
                document.getElementById("password").focus();
            }else{
                alert("Account deleted!")
            }
        }
    }
    xmlHttp.send(email+"\n"+hash);
    return false;
}