// Copyright © 2014 John Watson
// Licensed under the terms of the MIT License

var GameState = function(game) {
};

// Load images and sounds
GameState.prototype.preload = function() {
    this.game.load.image('cloud', 'assets/gfx/ball.png');
    this.game.load.image('ground', 'assets/gfx/ground.png');
};

// Setup the example
GameState.prototype.create = function() {
    // Set stage background color
    this.game.stage.backgroundColor = 0x4488cc;

    // Let's make some clouds
    for(var x = -56; x < this.game.width; x += 80) {
        var cloud = this.game.add.image(x, -80, 'cloud');
        cloud.scale.setTo(5, 5); // Make the clouds big
        cloud.tint = 0xcccccc; // Make the clouds dark
        cloud.smoothed = false; // Keeps the sprite pixelated
    }

    // Create some ground
    for(x = 0; x < this.game.width; x += 32) {
        this.game.add.image(x, this.game.height - 32, 'ground');
    }

    // Create a bitmap for the lightning bolt texture
    this.lightningBitmap = this.game.add.bitmapData(200, 338);

    // Create a sprite to hold the lightning bolt texture
    this.lightning = this.game.add.image(this.game.width/2, 80, this.lightningBitmap);

    // Set the anchor point of the sprite to center of the top edge
    // This allows us to position the lightning by simply specifiying the
    // x and y coordinate of where we want the lightning to appear from.
    this.lightning.anchor.setTo(0.5, 0);

    // Trigger lightning on mouse clicks and taps
    this.game.input.onTap.add(this.zap, this);
};

// The update() method is called every frame
GameState.prototype.update = function() {
};

// Create a lightning bolt
GameState.prototype.zap = function() {
    // Create the lightning texture
    this.createLightningTexture(this.lightningBitmap.width/2, 0, 20, 3, false);

    // Make the lightning sprite visible
    this.lightning.alpha = 1;

    // Fade out the lightning sprite using a tween on the alpha property
    // Check out the "Easing function" examples for more info.
    this.game.add.tween(this.lightning)
        .to({ alpha: 0.5 }, 100, Phaser.Easing.Bounce.Out)
        .to({ alpha: 1.0 }, 100, Phaser.Easing.Bounce.Out)
        .to({ alpha: 0.5 }, 100, Phaser.Easing.Bounce.Out)
        .to({ alpha: 1.0 }, 100, Phaser.Easing.Bounce.Out)
        .to({ alpha: 0 }, 250, Phaser.Easing.Cubic.In)
        .start();
};

// This function creates a texture that looks like a lightning bolt
GameState.prototype.createLightningTexture = function(x, y, segments, boltWidth, branch) {
    // Get the canvas drawing context for the lightningBitmap
    var ctx = this.lightningBitmap.context;
    var width = this.lightningBitmap.width;
    var height = this.lightningBitmap.height;

    // Our lightning will be made up of several line segments starting at
    // the center of the top edge of the bitmap and ending at the bottom edge
    // of the bitmap.

    // Clear the canvas
    if (!branch) ctx.clearRect(0, 0, width, height);

    // Draw each of the segments
    for(var i = 0; i < segments; i++) {
        // Set the lightning color and bolt width
        ctx.strokeStyle = 'rgb(255, 255, 255)';
        ctx.lineWidth = boltWidth;

        ctx.beginPath();
        ctx.moveTo(x, y);

        // Calculate an x offset from the end of the last line segment and
        // keep it within the bounds of the bitmap
        if (branch) {
            // For a branch
            x += this.game.rnd.integerInRange(-10, 10);
        } else {
            // For the main bolt
            x += this.game.rnd.integerInRange(-30, 30);
        }
        if (x <= 10) x = 10;
        if (x >= width-10) x = width-10;

        // Calculate a y offset from the end of the last line segment.
        // When we've reached the ground or there are no more segments left,
        // set the y position to the height of the bitmap. For branches, we
        // don't care if they reach the ground so don't set the last coordinate
        // to the ground if it's hanging in the air.
        if (branch) {
            // For a branch
            y += this.game.rnd.integerInRange(10, 20);
        } else {
            // For the main bolt
            y += this.game.rnd.integerInRange(20, height/segments);
        }
        if ((!branch && i == segments - 1) || y > height) {
            y = height;
        }

        // Draw the line segment
        ctx.lineTo(x, y);
        ctx.stroke();

        // Quit when we've reached the ground
        if (y >= height) break;

        // Draw a branch 20% of the time off the main bolt only
        if (!branch) {
            if (this.game.math.chanceRoll(20)) {
                // Draws another, thinner, bolt starting from this position
                this.createLightningTexture(x, y, 10, 1, true);
            }
        }
    }

    // This just tells the engine it should update the texture cache
    this.lightningBitmap.dirty = true;
};

// Setup game
var game = new Phaser.Game(640, 320, Phaser.AUTO, 'game');
game.state.add('game', GameState, true);
