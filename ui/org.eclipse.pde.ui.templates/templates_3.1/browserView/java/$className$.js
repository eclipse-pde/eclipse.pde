%if game == "no"
// Call from Java to set the random background color
function changeColor() {
	var x = Math.floor(Math.random() * 256);
	var y = Math.floor(Math.random() * 256);
	var z = Math.floor(Math.random() * 256);
	var bgColor = "rgb(" + x + "," + y + "," + z + ")";
	document.body.style.background = bgColor;
	document.getElementById("lastAction").innerText = "Background color set to " + bgColor;
}

// Call from Java to set the current selection
function setSelection(text) {
	document.getElementById("selection").innerText = text;
	document.getElementById("lastAction").innerText = "Selection set to " + text;
}

// Call to Java to open the preferences
function openPreferences() {
	try {
		var result = openEclipsePreferences(); // Java callback
		document.getElementById("lastAction").innerText = "Preferences were opened. Return value was: " + result;
	} catch (e) {
		document.getElementById("lastAction").innerText = "A Java error occured: " + e.message;
	}
}

// Call from java to say something
function say(something) {
	alert("Java says: " + something);
	document.getElementById("lastAction").innerText = "We said: " + something;
}
%else

// Game
    var ballRadius = 10;
    var paddleHeight = 10;
    var paddleWidth = 75;
    var brickWidth = 75;
    var brickHeight = 20;
    var brickPadding = 10;
    var brickOffsetTop = 30;
    var brickOffsetLeft = 30;
    var canvas = document.getElementById("myCanvas");
		canvas.width= window.innerWidth;
		canvas.height=window.innerHeight;
    var ctx = canvas.getContext("2d");
    var x = canvas.width-brickOffsetLeft;
    var y = canvas.height-brickOffsetTop;
    var paddleX = (canvas.width-paddleWidth)/2;
    var rightPressed = false;
    var leftPressed = false;
    var brickRowCount = Math.floor(x  / (brickWidth + brickPadding));
    var brickColumnCount = Math.floor((y  / 2) / (brickHeight + brickPadding));
    var dx = Math.floor(y/65);
    var dy = -dx;
    var score = 0;
    var lives = 3;

    var bricks = [];
    for(var c=0; c<brickColumnCount; c++) {
        bricks[c] = [];
        for(var r=0; r<brickRowCount; r++) {
            bricks[c][r] = { x: 0, y: 0, status: 1 };
        }
    }

    document.addEventListener("keydown", keyDownHandler, false);
    document.addEventListener("keyup", keyUpHandler, false);
    document.addEventListener("mousemove", mouseMoveHandler, false);

    function keyDownHandler(e) {
        if(e.code  == "ArrowRight") {
            rightPressed = true;
        }
        else if(e.code == 'ArrowLeft') {
            leftPressed = true;
        }
    }
    function keyUpHandler(e) {
        if(e.code == 'ArrowRight') {
            rightPressed = false;
        }
        else if(e.code == 'ArrowLeft') {
            leftPressed = false;
        }
    }
    function mouseMoveHandler(e) {
        var relativeX = e.clientX - canvas.offsetLeft;
        if(relativeX > 0 && relativeX < canvas.width) {
            paddleX = relativeX - paddleWidth/2;
        }
    }
    function collisionDetection() {
        for(var c=0; c<brickColumnCount; c++) {
            for(var r=0; r<brickRowCount; r++) {
                var b = bricks[c][r];
                if(b.status == 1) {
                    if(x > b.x && x < b.x+brickWidth && y > b.y && y < b.y+brickHeight) {
                        dy = -dy;
                        b.status = 0;
                        score++;
                        if(score == brickRowCount*brickColumnCount) {
                            gameOver("YOU WON!")
                            return true;
                        }
                    }
                }
            }
        }
		return false;
    }

    function drawBall() {
        ctx.beginPath();
        ctx.arc(x, y, ballRadius, 0, Math.PI*2);
        ctx.fillStyle = "#0095DD";
        ctx.fill();
        ctx.closePath();
    }
    function drawPaddle() {
        ctx.beginPath();
        ctx.rect(paddleX, canvas.height-paddleHeight, paddleWidth, paddleHeight);
        ctx.fillStyle = "#0095DD";
        ctx.fill();
        ctx.closePath();
    }
    function drawBricks() {
        for(var c=0; c<brickColumnCount; c++) {
            for(var r=0; r<brickRowCount; r++) {
                if(bricks[c][r].status == 1) {
                    var brickX = (r*(brickWidth+brickPadding))+brickOffsetLeft;
                    var brickY = (c*(brickHeight+brickPadding))+brickOffsetTop;
                    bricks[c][r].x = brickX;
                    bricks[c][r].y = brickY;
                    ctx.beginPath();
                    ctx.rect(brickX, brickY, brickWidth, brickHeight);
                    ctx.fillStyle = "#0095DD";
                    ctx.fill();
                    ctx.closePath();
                }
            }
        }
    }
	function changeColor() {
		var x = Math.floor(Math.random() * 256);
		var y = Math.floor(Math.random() * 256);
		var z = Math.floor(Math.random() * 256);
		var bgColor = "rgb(" + x + "," + y + "," + z + ")";
		document.body.style.background = bgColor;
		document.getElementById("lastAction").innerText = "Background color set to " + bgColor;
	}
    function drawScore() {
        ctx.font = "16px Arial";
        ctx.fillStyle = "#0095DD";
        ctx.fillText("Score: "+score, 8, 20);
    }
    function drawLives() {
        ctx.font = "16px Arial";
        ctx.fillStyle = "#0095DD";
        ctx.fillText("Lives: "+lives, canvas.width-65, 20);
    }

    function draw() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        drawBricks();
        drawBall();
        drawPaddle();
        drawScore();
        drawLives();
		if(collisionDetection()){
			return;
		}

        if(x + dx > canvas.width-ballRadius || x + dx < ballRadius) {
            dx = -dx;
        }
        if(y + dy < ballRadius) {
            dy = -dy;
        }
        else if(y + dy > canvas.height-ballRadius) {
            if(x > paddleX && x < paddleX + paddleWidth) {
                dy = -dy;
            }
            else {
                lives--;
                if(!lives) {
                    gameOver("YOU LOST!");
                    return;
                }
                else {
                    x = canvas.width/2;
                    y = canvas.height-30;
                    dx = Math.floor((canvas.height-brickOffsetTop)/65);
    				dy = -dx;
                    paddleX = (canvas.width-paddleWidth)/2;
                }
            }
        }

        if(rightPressed && paddleX < canvas.width-paddleWidth) {
            paddleX += 7;
        }
        else if(leftPressed && paddleX > 0) {
            paddleX -= 7;
        }

        x += dx;
        y += dy;
        requestAnimationFrame(draw);
    }

    function gameOver(text){
		ctx.font="30px Consolas";
		ctx.fillStyle = "red";
		ctx.textAlign = "center";
		ctx.fillText(text, canvas.width / 2, (canvas.height / 8) * 6);
		ctx.font="10px Consolas";
		ctx.fillStyle = "blue";
		ctx.textAlign = "center";
		ctx.fillText("(game code donated by https://github.com/end3r)", canvas.width/2, (canvas.height / 8) * 5);
	}

    draw();
%endif