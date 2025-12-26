let token = localStorage.getItem('token');

// 初始化
if (token) {
    showBookSection();
    loadBooks();
}

function api(method, url, body) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;
    return fetch(url, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined
    }).then(r => r.json());
}

function login() {
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;
    api('POST', '/api/auth/login', { username, password }).then(data => {
        if (data.token) {
            token = data.token;
            localStorage.setItem('token', token);
            showBookSection();
            loadBooks();
        } else {
            alert(data.error || '登录失败');
        }
    });
}

function logout() {
    api('POST', '/api/auth/logout').then(() => {
        token = null;
        localStorage.removeItem('token');
        location.reload();
    });
}

function showBookSection() {
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('book-section').style.display = 'block';
    document.getElementById('user-info').style.display = 'flex';
    api('GET', '/api/auth/me').then(data => {
        document.getElementById('username').textContent = '欢迎, ' + data.username;
    });
}

function loadBooks(keyword) {
    const url = keyword ? '/api/books?q=' + encodeURIComponent(keyword) : '/api/books';
    api('GET', url).then(data => {
        const list = document.getElementById('book-list');
        if (!data.books || data.books.length === 0) {
            list.innerHTML = '<p style="color:#999;padding:20px;">暂无图书</p>';
            return;
        }
        list.innerHTML = data.books.map(book => `
            <div class="book-item">
                <img class="book-cover" src="${book.coverImage || '/static/no-cover.svg'}" alt="封面">
                <div class="book-info">
                    <h3>${book.title}</h3>
                    <p>作者: ${book.author || '未知'}</p>
                    <p>ISBN: ${book.isbn || '-'}</p>
                    <p>${book.description || ''}</p>
                    <div class="book-actions">
                        <button class="delete" onclick="deleteBook(${book.id})">删除</button>
                    </div>
                </div>
            </div>
        `).join('');
    });
}

function searchBooks() {
    const keyword = document.getElementById('search').value;
    loadBooks(keyword);
}

function addBook() {
    const title = document.getElementById('book-title').value;
    const author = document.getElementById('book-author').value;
    const isbn = document.getElementById('book-isbn').value;
    const description = document.getElementById('book-desc').value;
    const fileInput = document.getElementById('book-cover');
    
    if (!title) { alert('请输入书名'); return; }
    
    const saveBook = (coverImage) => {
        api('POST', '/api/books', { title, author, isbn, description, coverImage }).then(data => {
            if (data.id) {
                document.getElementById('book-title').value = '';
                document.getElementById('book-author').value = '';
                document.getElementById('book-isbn').value = '';
                document.getElementById('book-desc').value = '';
                fileInput.value = '';
                loadBooks();
            } else {
                alert(data.error || '添加失败');
            }
        });
    };
    
    if (fileInput.files.length > 0) {
        const formData = new FormData();
        formData.append('file', fileInput.files[0]);
        fetch('/api/upload/cover', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + token },
            body: formData
        }).then(r => r.json()).then(data => {
            saveBook(data.url);
        });
    } else {
        saveBook(null);
    }
}

function deleteBook(id) {
    if (!confirm('确定删除?')) return;
    api('DELETE', '/api/books/' + id).then(() => loadBooks());
}
