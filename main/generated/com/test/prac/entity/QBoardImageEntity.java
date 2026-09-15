package com.test.prac.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoardImageEntity is a Querydsl query type for BoardImageEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoardImageEntity extends EntityPathBase<BoardImageEntity> {

    private static final long serialVersionUID = -1125815928L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoardImageEntity boardImageEntity = new QBoardImageEntity("boardImageEntity");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final QBoardEntity board;

    public final NumberPath<Long> boardId = createNumber("boardId", Long.class);

    public final NumberPath<Long> imageId = createNumber("imageId", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> moddate = _super.moddate;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regdate = _super.regdate;

    public QBoardImageEntity(String variable) {
        this(BoardImageEntity.class, forVariable(variable), INITS);
    }

    public QBoardImageEntity(Path<? extends BoardImageEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoardImageEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoardImageEntity(PathMetadata metadata, PathInits inits) {
        this(BoardImageEntity.class, metadata, inits);
    }

    public QBoardImageEntity(Class<? extends BoardImageEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.board = inits.isInitialized("board") ? new QBoardEntity(forProperty("board"), inits.get("board")) : null;
    }

}

