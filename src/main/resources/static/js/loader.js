//loader text array of common programming puns
var content=[];
content[0]= "Waking the core...";
content[1]= "Syncing with the shadows...";
content[2]="Establishing void links...";

var loader = document.getElementById('load-text');
var contentIndex = 0;

//function to update the content of the div by choosing a random element from the above array 
function updateContent() {
    contentIndex = Math.floor(Math.random()*content.length);
    loader.innerHTML = content[contentIndex];

}

setInterval(updateContent, 800);

updateContent();

//loader transition and visibility
window.onload = function() {
    setTimeout(function() {
        document.getElementById("load-cont").style.display = "none";
    }, 3000);
}