const mailFormat = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;

const verificator = new URL(window.location.href).searchParams.get("verificator");
if(verificator !== null){
    document.getElementById("alreadyHaveAccountQ").setAttribute("display","none");
    document.getElementById("signUpB").innerHTML = "<strong>Reset password</strong>";
    document.getElementById("signUpB").setAttribute("onclick", "resetPassword()");
}

function register(){
    const email = document.getElementById("email").value.toLowerCase();
    const password = document.getElementById("password").value;
    const passwordCheck = document.getElementById("passwordCheck").value;

    if(!validateEmail(email)){
        alert("You have entered an invalid email address!");
        document.getElementById("email").focus();
        return false;
    }

    if(password !== passwordCheck){
        alert("Passwords does not match!");
        return false;
    }

    if(password === ""){
        alert("You have not entered your password!");
        return false;
    }

    const hash = CryptoJS.SHA256(email+password);

    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/register" , true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            //location.reload();
        }else{
            if(xmlHttp.responseText === "true"){
                alert("Check your mailbox and confirm your email.")
                window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/login.html")
            }else{
                alert("You have already registered for this email.")
            }
        }
    }
    xmlHttp.send(email+"\n"+hash);
    return false;
}

function validateEmail(inputText) {
    return mailFormat.test(inputText);
}

function resetPassword(){
    const email = document.getElementById("email").value.toLowerCase();
    const password = document.getElementById("password").value;
    const passwordCheck = document.getElementById("passwordCheck").value;

    if(!validateEmail(email)){
        alert("You have entered an invalid email address!");
        document.getElementById("email").focus();
        return false;
    }

    if(password !== passwordCheck){
        alert("Passwords does not match!");
        return false;
    }

    if(password !== ""){
        alert("You have not entered your password!");
        return false;
    }

    const hash = CryptoJS.SHA256(email+password);

    let xmlHttp = new XMLHttpRequest();
    xmlHttp.open("POST", window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/resetpassword" , true ); // false for synchronous request
    xmlHttp.onload = () =>{
        if(xmlHttp.status !== 200){
            alert("Connection error");
            //location.reload();
        }else{
            if(xmlHttp.responseText === "success"){
                alert("Password changed.");
                window.location.assign(window.location.protocol + "//" + window.location.hostname + ":" + window.location.port + "/login.html")
            }else if("email"){
                alert("This email is not registered");
            }else{
                alert("Error, check your email or try request for another reset email.")
            }
        }
    }
    xmlHttp.send(email+"\n"+verificator+"\n"+hash);
}