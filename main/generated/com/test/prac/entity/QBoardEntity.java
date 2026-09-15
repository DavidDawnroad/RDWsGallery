package com.test.prac.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoardEntity is a Querydsl query type for BoardEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoardEntity extends EntityPathBase<BoardEntity> {

    private static final long serialVersionUID = 2119051577L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoardEntity boardEntity = new QBoardEntity("boardEntity");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<Long> boardSeq = createNumber("boardSeq", Long.class);

    public final EnumPath<com.test.prac.enums.Category> category = createEnum("category", com.test.prac.enums.Category.class);

    public final StringPath content = createString("content");

    public final NumberPath<Long> likes = createNumber("likes", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> moddate = _super.moddate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regdate = _super.regdate;

    public final StringPath title = createString("title");

    public final QUserAccountEntity userEntity;

    public final NumberPath<Long> views = createNumber("views", Long.class);

    public QBoardEntity(String variable) {
        this(BoardEntity.class, forVariable(variable), INITS);
    }

    public QBoardEntity(Path<? extends BoardEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoardEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoardEntity(PathMetadata metadata, PathInits inits) {
        this(BoardEntity.class, metadata, inits);
    }

    public QBoardEntity(Class<? extends BoardEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.userEntity = inits.isInitialized("userEntity") ? new QUserAccountEntity(forProperty("userEntity")) : null;
    }

}

