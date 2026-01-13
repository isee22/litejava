/**
 * 房间管理器
 */
class RoomManager {
    constructor(maxPlayers) {
        this.maxPlayers = maxPlayers;
        this.rooms = new Map();
        this.userRoom = new Map();
    }

    createRoom(roomId, ownerId) {
        const room = {
            id: roomId,
            ownerId,
            game: null,
            seats: Array.from({ length: this.maxPlayers }, (_, i) => ({
                seatIndex: i,
                userId: 0,
                name: '',
                ready: false,
                online: false
            }))
        };
        this.rooms.set(roomId, room);
        return room;
    }

    getRoom(roomId) {
        return this.rooms.get(roomId);
    }

    hasRoom(roomId) {
        return this.rooms.has(roomId);
    }

    deleteRoom(roomId) {
        const room = this.rooms.get(roomId);
        if (room) {
            room.seats.forEach(s => {
                if (s.userId) this.userRoom.delete(s.userId);
            });
            this.rooms.delete(roomId);
        }
    }

    enterRoom(roomId, userId, name) {
        const room = this.rooms.get(roomId);
        if (!room) return { error: 'ROOM_NOT_FOUND' };

        let seated = room.seats.some(s => s.userId === userId);
        if (!seated) {
            const empty = room.seats.find(s => !s.userId);
            if (!empty) return { error: 'ROOM_FULL' };
            empty.userId = userId;
            empty.name = name;
        }

        this.userRoom.set(userId, roomId);
        return { room };
    }

    exitRoom(userId) {
        const roomId = this.userRoom.get(userId);
        if (!roomId) return null;

        const room = this.rooms.get(roomId);
        if (room) {
            const seat = room.seats.find(s => s.userId === userId);
            if (seat) {
                seat.userId = 0;
                seat.name = '';
                seat.ready = false;
                seat.online = false;
            }
        }
        this.userRoom.delete(userId);
        return room;
    }

    getUserRoom(userId) {
        return this.userRoom.get(userId);
    }

    getSeatIndex(room, userId) {
        return room.seats.findIndex(s => s.userId === userId);
    }

    getSeat(room, userId) {
        return room.seats.find(s => s.userId === userId);
    }

    setOnline(room, userId, online) {
        const seat = room.seats.find(s => s.userId === userId);
        if (seat) seat.online = online;
        return seat;
    }

    setReady(room, userId, ready) {
        const seat = room.seats.find(s => s.userId === userId);
        if (seat) seat.ready = ready;
        return seat;
    }

    getPlayerCount(room) {
        return room.seats.filter(s => s.userId).length;
    }

    isAllReady(room) {
        const players = room.seats.filter(s => s.userId);
        return players.length === this.maxPlayers && players.every(s => s.ready);
    }

    resetRoom(room) {
        room.game = null;
        room.seats.forEach(s => s.ready = false);
    }

    get roomCount() {
        return this.rooms.size;
    }

    get playerCount() {
        return this.userRoom.size;
    }
}

module.exports = RoomManager;
