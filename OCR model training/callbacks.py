import tensorflow as tf

class accuracyCallback(tf.keras.callbacks.Callback):

    def __init__(self, accuracy) -> None:
        super().__init__()
        self.accuracy = accuracy

    def on_epoch_end(self, epoch, logs={}):
        if logs.get("accuracy") > self.accuracy:
            print("\nTarget accuracy of %f reached." % (self.accuracy))
            self.model.stop_training = True


class customLearningRateScheduler(tf.keras.callbacks.Callback):

    def __init__(self, schedule):
        super(customLearningRateScheduler, self).__init__()
        self.schedule = schedule
    
    def on_epoch_begin(self, epoch, logs=None):
        epoch += 1
        if not hasattr(self.model.optimizer, "lr"):
            raise ValueError('Optimizer must have a "lr" attribute.')
        # Get the current learning rate from model's optimizer.
        lr = float(tf.keras.backend.get_value(self.model.optimizer.learning_rate))
        # Call schedule function to get the scheduled learning rate.
        scheduled_lr = self.schedule(epoch, lr)
        # Set the value back to the optimizer before this epoch starts
        tf.keras.backend.set_value(self.model.optimizer.lr, scheduled_lr)
        print("\nEpoch %05d: Learning rate is %6.4f." % (epoch, scheduled_lr))



LR_SCHEDULE = [
    # (epoch to start, learning rate) tuples
    (10, 0.05),
    (20, 0.01),
    (30, 0.005),
    (40, 0.001),
]

def lr_schedule(epoch, lr):
    """Helper function to retrieve the scheduled learning rate based on epoch."""
    if epoch < LR_SCHEDULE[0][0] or epoch > LR_SCHEDULE[-1][0]:
        return lr
    for i in range(len(LR_SCHEDULE)):
        if epoch == LR_SCHEDULE[i][0]:
            return LR_SCHEDULE[i][1]
    return lr